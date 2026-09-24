#!/usr/bin/env python3
"""
CTDI 伤害机制实机回归测试（RCON）

用途：在一台跑着 CTDI 的 Forge 1.20.1 服务端上，验证"无敌帧已被移除"。
原理：对同一只僵尸连续打两次相同的 5 点伤害（两次命令间隔约 1 tick，落在原版
10 tick 的无敌帧窗口内）。

预期结果：
  有 CTDI  ：20.0f -> 10.0f   （两次伤害全部生效）
  无 CTDI  ：20.0f -> 15.0f   （第二次被原版无敌帧丢弃，这就是对照组）

前置条件（服务端 server.properties）：
  enable-rcon=true
  rcon.port=25575
  rcon.password=ctditest
  broadcast-rcon-to-ops=false

用法（在服务端同机执行）：
  python3 tools/damage_test.py WITH-CTDI
  # 想看对照组：把 mods/ctdi-*.jar 移出 mods/，重启服务端，再跑一次

只依赖 Python 标准库，不需要额外安装任何东西。
"""

import socket
import struct
import sys
import time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "ctditest"


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
    r.cmd("forceload add 0 0")
    r.cmd("kill @e[type=minecraft:zombie]")
    time.sleep(0.5)
    r.cmd('summon minecraft:zombie 8 100 8 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,'
          'CustomName:\'{"text":"ctditest"}\'}')
    time.sleep(0.5)
    sel = "@e[type=minecraft:zombie,limit=1]"
    before = r.cmd(f"data get entity {sel} Health")
    # 两次相同伤害，约 1 tick 间隔 => 落在原版 10 tick 无敌帧窗口内
    r.cmd(f"damage {sel} 5 minecraft:generic")
    r.cmd(f"damage {sel} 5 minecraft:generic")
    time.sleep(0.5)
    after = r.cmd(f"data get entity {sel} Health")
    print(f"[{tag}] before: {before}")
    print(f"[{tag}] after 2x5 dmg: {after}")
    r.cmd("kill @e[type=minecraft:zombie]")
    r.cmd("forceload remove all")


if __name__ == "__main__":
    main()
