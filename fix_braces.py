import re

file_path = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"

with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "//" in line:
        parts = line.split("//", 1)
        code_part = parts[0]
        comment_part = parts[1]
        
        brace_count = comment_part.count('}')
        if brace_count > 0:
            new_lines.append(code_part + "\n")
            for _ in range(brace_count):
                new_lines.append("    }\n")
        else:
            new_lines.append(code_part + "\n")
    else:
        new_lines.append(line)

with open(file_path, "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print("Fixed braces and removed comments.")
