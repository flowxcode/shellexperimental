## Basics
> hardware, 280 solves, 50 points

In this challenge we're given an URL to connect to and two files:
- `check.sv`: a file containing [SystemVerilog](https://en.wikipedia.org/wiki/SystemVerilog) which
  is a *hardware description language* (HDL). In this case it describes a password checker which
  receives input characters individually and outputs a single boolean signal indicating whether the
  password was correct.
- `main.cpp`: a wrapper program to drive the password checker using
  [Verilator](https://www.veripool.org/wiki/verilator) - a SystemVerilog simulator. We only interact
  with this program. It drives the clock of the password checker and forwards our input to it. If
  the password is correct we're rewarded with the flag.

Although not strictly necessary I found it useful to be able to run the simulation locally with
changes applied to `check.sv` (e.g. to check whether the output was `true` if a wire was fixed to
some value). To do this we can compile the files with `verilator`:

```sh
# Compile
verilator -Wall --cc check.sv --exe --build main.cpp
# And run
./obj_dir/Vcheck
```

Let's take a closer look at `check.sv`:

```SystemVerilog
module check(
    input clk,

    input [6:0] data,
    output wire open_safe
);

reg [6:0] memory [7:0];
reg [2:0] idx = 0;

wire [55:0] magic = {
    {memory[0], memory[5]},
    {memory[6], memory[2]},
    {memory[4], memory[3]},
    {memory[7], memory[1]}
};

wire [55:0] kittens = { magic[9:0],  magic[41:22], magic[21:10], magic[55:42] };
assign open_safe = kittens == 56'd3008192072309708;

always_ff @(posedge clk) begin
    memory[idx] <= data;
    idx <= idx + 5;
end

endmodule
```

It defines a module `check` with two inputs (`clk` and `data`) and one output (`open_safe`):
- `clk` is the clock signal which gets driven by `main.cpp`
- `data` is the latest password character which we've given; it consists of 7 bit which suffices for
  ASCII (the 8th bit would always be 0 anyways)
- `open_safe` becomes true if the password was correct

In addition to that we have two registers (they store their values across clock cycles):
- `memory` is an array of 8 7-bit values
- `idx` is a 3-bit number which means it wraps around from 7 to 0

The wires are the main logic of the password checker. They shuffle around the bits from the `memory`
register and if the value then matches a specific constant `open_safe` is set to `1`.

At last we see the `always_ff` block which gets triggered each time a positive edge (`0` -> `1`) is
detected on the `clk` signal. It simply sets the `memory[idx]` to the current `data` input and then
advances the `idx` by 5 (so `idx = (idx + 5) % 8` due to the size of `idx`).

Now that we know what the module does the plan of attack is simple: We just work backwards from the
`open_safe` signal and determine which values must be in `memory`. Then we just have to input the
determined in the correct order (since the `idx` doesn't just increment by one) and we should get
the flag. I did this manually using my text editor:

1. Determine `kittens`; Since `kittens == 56'd3008192072309708` (`56'd` means a 56-bit value given
   in decimal) we just take the binary representation of `3008192072309708`:
```
00001010101011111110111101001011111000101101101111001100
```
2. Determine `magic`; `kittens` consists of parts of `magic`:
```
0000101010 10111111101111010010 111110001011 01101111001100
magic[9:0] magic[41:22]         magic[21:10] magic[55:42]
01101111001100101111111011110100101111100010110000101010
```
3. Determine `memory`: `magic` just shuffles around the ASCII values from `memory` (beware that
   index `0` is the rightmost value since SystemVerilog is a HDL and the 0th bit is the rightmost
   bit):
```
0110111 1001100 1011111 1101111 0100101 1111000 1011000 0101010
0       5       6       2       4       3       7       1
1011000 1011111 1001100 0100101 1111000 1101111 0101010 0110111
7       6       5       4       3       2       1       0
```

We could reverse the `idx` shuffling manually but in the end I just wrote this script to get us the
flag:

```python
#!/usr/bin/env python3

import socket
import struct

vals = [
    0b0110111,
    0b0101010,
    0b1101111,
    0b1111000,
    0b0100101,
    0b1001100,
    0b1011111,
    0b1011000,
]

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(("basics.2020.ctfcompetition.com", 1337))
print(sock.recv(4096).decode().strip())

password = ""
idx = 0
for _ in range(8):
    password += chr(vals[idx])
    idx = (idx + 5) % 8
print(password)

sock.sendall(password.encode())
sock.sendall(b"\n")
print(sock.recv(4096).decode().strip())
```

```
Enter password:
7LoX%*_x
CTF{W4sTh4tASan1tyCh3ck?}
```