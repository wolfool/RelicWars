import os
import re

replacements = {
    "Particle.EXPLOSION_LARGE": "Particle.EXPLOSION",
    "Particle.SPELL_WITCH": "Particle.WITCH",
    "Particle.ENCHANTMENT_TABLE": "Particle.ENCHANT",
    "Particle.EXPLOSION_HUGE": "Particle.EXPLOSION_EMITTER",
    "Particle.SMOKE_LARGE": "Particle.LARGE_SMOKE",
    "Particle.TOTEM": "Particle.TOTEM_OF_UNDYING",
    "Particle.CRIT_MAGIC": "Particle.ENCHANTED_HIT",
    "Attribute.GENERIC_MAX_HEALTH": "Attribute.MAX_HEALTH",
    "FootprintTracker.Queue": "java.util.Queue",
    "RelicItemUtil.": "com.wolfool.relicwars.relic.RelicItemUtil."
}

def process_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements.items():
        new_content = new_content.replace(old, new)
        
    if content != new_content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk("src/main/java"):
    for file in files:
        if file.endswith(".java"):
            process_file(os.path.join(root, file))

print("Done updating API symbols.")
