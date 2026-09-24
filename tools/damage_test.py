#!/usr/bin/env python3
"""
CTDI 伤害机制实机回归测试（RCON）

用途：在一台跑着 CTDI 的 Forge 1.20.1 服务端上，验证"无敌帧已被移除"。
原理：对同一只僵尸连续打两次相同的 5 点伤害，两次都落在原版 10 tick 的
无敌帧窗口内（约 0.5 秒）。

预期结果：
  有 CTDI  ：20.0f -> 10.0f   （两次伤害全部生效）
  无 CTDI  ：20.0f -> 15.0f   （第二次被原版无敌帧丢弃，这就是对照组）

两种打击模式：
  same-tick（更严格，推荐）：调用数据包函数 ctditest:double_hit，
      函数内两条 damage 命令在**同一个服务端 tick** 内执行，
      用来验证"同 tick 多次命中"这种最极端的情况。
      需要服务端有该函数，见下方"同 tick 测试数据包"。
  window（兜底）：连发两条 RCON 命令（间隔约 1 tick）。
      数据包函数不存在时脚本自动退化为这个模式。

前置条件（服务端 server.properties）：
  enable-rcon=true
  rcon.port=25575
  rcon.password=ctditest
  broadcast-rcon-to-ops=false

同 tick 测试数据包（放到 <服务端>/world/datapacks/ctditest/）：
  pack.mcmeta:
    {"pack": {"pack_format": 15, "description": "CTDI test"}}
  data/ctditest/functions/double_hit.mcfunction:
    damage @e[type=minecraft:zombie,limit=1] 5 minecraft:generic
    damage @e[type=minecraft:zombie,limit=1] 5 minecraft:generic
  放好后用 RCON 执行一次 /reload 即可生效。

用法（在服务端同机执行）：
  python3 tools/damage_test.py WITH-CTDI
  # 想看对照组：把 mods/ctdi-*.jar 移出 mods/，重启服务端，再跑一次

多服务端并行时用环境变量指定连接参数：
  CTDI_RCON_HOST=127.0.0.1 CTDI_RCON_PORT=25576 CTDI_RCON_PASSWORD=xxx \
      python3 tools/damage_test.py FABRIC-1.20.1
  默认值：127.0.0.1 / 25575 / ctditest

只依赖 Python 标准库，不需要额外安装任何东西。
"""

import os
import socket
import struct
import sys
import time

# 默认值可用环境变量覆盖（多版本/多服务端并行测试时很方便）：
#   CTDI_RCON_HOST / CTDI_RCON_PORT / CTDI_RCON_PASSWORD
HOST = os.environ.get("CTDI_RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("CTDI_RCON_PORT", "25575"))
PASSWORD = os.environ.get("CTDI_RCON_PASSWORD", "ctditest")


class Rcon:
    def __init__(self):
        self.s = socket.create_connection((HOST, PORT), timeout=15)
        self.rid = 0
        self._send(3, PASSWORD)
        rid, _ = self._recv()
        if rid == -1:
            raise SystemExit("RCON auth failed")

    def _send(self, ptype, payload):
        self.rid += 1
        data = struct.pack("<ii", self.rid, ptype) + payload.encode("utf8") + b"\x00\x00"
        self.s.sendall(struct.pack("<i", len(data)) + data)
        return self.rid

    def _recv(self):
        (length,) = struct.unpack("<i", self._read(4))
        data = self._read(length)
        rid, ptype = struct.unpack("<ii", data[:8])
        return rid, data[8:-2].decode("utf8", "replace")

    def _read(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.s.recv(n - len(buf))
            if not chunk:
                raise EOFError("connection closed")
            buf += chunk
        return buf

    def cmd(self, command):
        self._send(2, command)
        _, body = self._recv()
        return body.strip()


def main():
    tag = sys.argv[1] if len(sys.argv) > 1 else "run"
    r = Rcon()
    sel = "@e[type=minecraft:zombie,limit=1]"
    # 召唤 + 查找，失败就重试：服务端刚启动时目标区块可能还没加载，
    # 此时实体会落在未加载区块里、@e 选择器找不到它。
    #
    # 位置默认取高空（y=250）+ NoGravity：避免僵尸卡在方块里被"窒息伤害"污染测量
    # —— CTDI 移除无敌帧后环境伤害每 tick 都会生效，落在方块里会越打越掉血。
    # 可用 CTDI_TEST_POS="x y z" 覆盖。
    pos = os.environ.get("CTDI_TEST_POS", "8 250 8")
    before = ""
    for _ in range(20):
        r.cmd("forceload add 0 0")
        r.cmd("kill @e[type=minecraft:zombie]")
        time.sleep(0.3)
        r.cmd(f'summon minecraft:zombie {pos} {{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,'
              'CustomName:\'{"text":"ctditest"}\'}')
        time.sleep(0.5)
        before = r.cmd(f"data get entity {sel} Health")
        if "No entity" not in before and "没有实体" not in before:
            break
        time.sleep(1)
    else:
        print(f"[{tag}] 失败：召唤后仍找不到僵尸（区块未加载/命令语法问题）：{before}")
        return

    # 优先用数据包函数：函数内两条命令保证在同一个服务端 tick 执行（最严格的场景）
    mode = "same-tick (datapack function)"
    probe = r.cmd("function ctditest:double_hit")
    if "Unknown" in probe or "unknown" in probe or "Failed" in probe or "failed" in probe:
        mode = "window (two rapid RCON commands; datapack function not installed)"
        r.cmd(f"damage {sel} 5 minecraft:generic")
        r.cmd(f"damage {sel} 5 minecraft:generic")

    # 立刻读数（不再 sleep）：减少环境伤害等其他来源的干扰
    after = r.cmd(f"data get entity {sel} Health")
    print(f"[{tag}] mode: {mode}")
    print(f"[{tag}] before: {before}")
    print(f"[{tag}] after 2x5 dmg: {after}")
    r.cmd("kill @e[type=minecraft:zombie]")
    r.cmd("forceload remove all")


if __name__ == "__main__":
    main()
