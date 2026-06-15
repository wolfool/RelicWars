import sys

file_path = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"

with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
    lines = f.readlines()

brace_level = 0
for i, line in enumerate(lines):
    # Strip comments for brace counting
    code_part = line.split("//")[0]
    
    for char in code_part:
        if char == '{':
            brace_level += 1
        elif char == '}':
            brace_level -= 1
            if brace_level < 0:
                print(f"Error: Negative brace level at line {i+1}")

if brace_level != 0:
    print(f"Final brace level: {brace_level}")
else:
    print("Braces match!")
