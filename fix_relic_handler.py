import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
extracted = []

extracting = False
for i, line in enumerate(lines):
    if i == 646:
        extracting = True
    
    if extracting:
        extracted.append(line)
        if i == 665:
            extracting = False
    else:
        new_lines.append(line)

# Now insert extracted right before execute020Option2
for i, line in enumerate(new_lines):
    if "public void execute020Option2(Player player) {" in line:
        final_lines = new_lines[:i] + extracted + new_lines[i:]
        break

with open(filepath, "w", encoding="utf-8") as f:
    f.writelines(final_lines)
print("Fixed syntax error")
