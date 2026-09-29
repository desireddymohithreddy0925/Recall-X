import glob

def replace_colors(content):
    # Backgrounds
    content = content.replace("bg-slate-950", "bg-app-bg")
    content = content.replace("bg-slate-900/80", "bg-app-secondary/80")
    content = content.replace("bg-slate-900", "bg-app-secondary")
    content = content.replace("bg-slate-800", "bg-app-card")
    content = content.replace("hover:bg-slate-800", "hover:bg-app-card")
    content = content.replace("hover:bg-slate-700", "hover:bg-app-elevated")
    
    # Text
    content = content.replace("text-white", "text-app-text-primary")
    content = content.replace("text-slate-200", "text-app-text-primary")
    content = content.replace("text-slate-300", "text-app-text-primary")
    content = content.replace("text-slate-400", "text-app-text-secondary")
    content = content.replace("text-slate-500", "text-app-text-secondary")
    content = content.replace("text-slate-600", "text-app-text-secondary")
    content = content.replace("placeholder:text-slate-600", "placeholder:text-app-text-secondary")
    
    # Borders
    content = content.replace("border-slate-800", "border-app-border")
    content = content.replace("border-slate-700", "border-app-border")
    
    # Accents (Primary - Blue/Indigo)
    content = content.replace("text-blue-500", "text-app-accent")
    content = content.replace("text-blue-400", "text-app-accent")
    content = content.replace("text-indigo-500", "text-app-memory")
    content = content.replace("focus:border-blue-500", "focus:border-app-accent")
    content = content.replace("focus:ring-blue-500", "focus:ring-app-accent")
    content = content.replace("bg-blue-600", "bg-app-accent text-app-bg")
    content = content.replace("hover:bg-blue-700", "hover:opacity-90")
    content = content.replace("bg-blue-950/30", "bg-app-accent/10")
    content = content.replace("border-blue-900/50", "border-app-accent/30")
    content = content.replace("bg-blue-900/30", "bg-app-accent/20")
    content = content.replace("border-blue-800", "border-app-accent")
    
    # Accents (Warnings/Success/Errors)
    content = content.replace("text-orange-400", "text-app-warning")
    content = content.replace("text-amber-500", "text-app-warning")
    content = content.replace("focus:border-orange-500", "focus:border-app-warning")
    content = content.replace("text-green-500", "text-app-success")
    content = content.replace("text-green-400", "text-app-success")
    content = content.replace("focus:border-green-500", "focus:border-app-success")
    content = content.replace("text-red-500", "text-app-critical")
    content = content.replace("text-rose-500", "text-app-critical")

    # Fix double texts
    content = content.replace("text-app-bg font-medium text-app-text-primary", "text-app-bg font-medium")
    
    return content

files = glob.glob("src/pages/*.jsx")
for f in files:
    with open(f, "r") as file:
        content = file.read()
    
    new_content = replace_colors(content)
    
    with open(f, "w") as file:
        file.write(new_content)

print("Colors updated in pages!")
