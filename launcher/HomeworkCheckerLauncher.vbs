Set objShell = CreateObject("Wscript.Shell")
objShell.CurrentDirectory = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
objShell.Run "cmd /c app.bat", 0, True