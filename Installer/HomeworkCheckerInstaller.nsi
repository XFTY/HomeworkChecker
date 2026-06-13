; HomeworkCheckerInstaller.nsi
; HomeworkChecker 安装程序
; 作者: XFTY
; 使用 NSIS Modern UI 2
;
; *** 升级时只需修改下面两行 ***
!define PRODUCT_NAME "HomeworkChecker"
!define PRODUCT_VERSION "1.7.0-Beta"
!define PRODUCT_VERSION_DWORD "1.7.0.0"
!define PRODUCT_PUBLISHER "XFTY"

!include "MUI2.nsh"
!include "nsDialogs.nsh"
!include "LogicLib.nsh"

; ========== 基本设置 ==========

Name "${PRODUCT_NAME}"
OutFile "${PRODUCT_NAME}-${PRODUCT_VERSION}-Setup.exe"
RequestExecutionLevel admin

InstallDir "$PROGRAMFILES64\${PRODUCT_NAME}"
InstallDirRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "InstallLocation"

; ========== 版本信息 ==========

VIProductVersion "${PRODUCT_VERSION_DWORD}"
VIAddVersionKey "ProductName" "${PRODUCT_NAME}"
VIAddVersionKey "FileVersion" "${PRODUCT_VERSION}"
VIAddVersionKey "LegalCopyright" "${PRODUCT_PUBLISHER}"
VIAddVersionKey "FileDescription" "${PRODUCT_NAME} Installer"

; ========== 图标 ==========

Icon "InstallFiles\logo.ico"
UninstallIcon "InstallFiles\logo.ico"

; ========== 变量 ==========

Var InstallMode
Var SkipCustomPages
Var RadioAllUsers
Var RadioCurrentUser
Var ChkDesktop
Var ChkStartMenu
Var CreateDesktopShortcut
Var CreateStartMenuShortcut

; ========== 语言字符串 ==========

; --- 欢迎页面 ---
LangString STR_WELCOME_TITLE 2052 "欢迎使用 HomeworkChecker 安装向导"
LangString STR_WELCOME_TITLE 1028 "歡迎使用 HomeworkChecker 安裝精靈"
LangString STR_WELCOME_TITLE 1033 "Welcome to the HomeworkChecker Setup Wizard"

LangString STR_WELCOME_TEXT 2052 "本向导将引导您完成 HomeworkChecker 1.7.0-Beta 的安装。$\r$\n$\r$\n------$\r$\n$\r$\n建议您在安装前关闭其他应用程序，以便安装程序能够更新必要的系统文件。$\r$\n$\r$\n点击$\"下一步$\"继续。"
LangString STR_WELCOME_TEXT 1028 "本精靈將引導您完成 HomeworkChecker 1.7.0-Beta 的安裝。$\r$\n$\r$\n------$\r$\n$\r$\n建議您在安裝前關閉其他應用程式，以便安裝程式能夠更新必要的系統檔案。$\r$\n$\r$\n按一下「下一步」繼續。"
LangString STR_WELCOME_TEXT 1033 "This wizard will guide you through the installation of HomeworkChecker 1.7.0-Beta.$\r$\n$\r$\n------$\r$\n$\r$\nIt is recommended that you close all other applications before continuing.$\r$\n$\r$\nClick $\"Next$\" to continue."

; --- 许可协议页面 ---
LangString STR_LICENSE_TOP 2052 "请仔细阅读以下许可协议。在阅读协议后，您必须接受协议条款才能继续安装。"
LangString STR_LICENSE_TOP 1028 "請仔細閱讀以下授權合約。在閱讀合約後，您必須接受合約條款才能繼續安裝。"
LangString STR_LICENSE_TOP 1033 "Please review the license agreement. You must accept the terms to continue the installation."

LangString STR_LICENSE_CHECKBOX 2052 "我接受许可协议的条款"
LangString STR_LICENSE_CHECKBOX 1028 "我接受授權合約的條款"
LangString STR_LICENSE_CHECKBOX 1033 "I accept the terms of the License Agreement"

; --- 目录选择页面 ---
LangString STR_DIRECTORY_TOP 2052 "选择要将 HomeworkChecker 安装到哪个文件夹。$\r$\n安装程序将把程序文件安装到以下目录中。"
LangString STR_DIRECTORY_TOP 1028 "選擇要將 HomeworkChecker 安裝到哪個資料夾。$\r$\n安裝程式將把程式檔案安裝到以下目錄中。"
LangString STR_DIRECTORY_TOP 1033 "Select the folder where HomeworkChecker should be installed.$\r$\nThe installer will install the program files to the following directory."

; --- 完成页面 ---
LangString STR_FINISH_TITLE 2052 "正在完成 HomeworkChecker 安装向导"
LangString STR_FINISH_TITLE 1028 "正在完成 HomeworkChecker 安裝精靈"
LangString STR_FINISH_TITLE 1033 "Completing the HomeworkChecker Setup Wizard"

LangString STR_FINISH_TEXT 2052 "HomeworkChecker 1.7.0-Beta 已成功安装到您的计算机。$\r$\n$\r$\n点击$\"完成$\"关闭此向导。"
LangString STR_FINISH_TEXT 1028 "HomeworkChecker 1.7.0-Beta 已成功安裝到您的電腦。$\r$\n$\r$\n按一下「完成」關閉此精靈。"
LangString STR_FINISH_TEXT 1033 "HomeworkChecker 1.7.0-Beta has been successfully installed on your computer.$\r$\n$\r$\nClick $\"Finish$\" to close this wizard."

; --- 卸载确认页面 ---
LangString STR_UNCONFIRM_TOP 2052 "即将从您的计算机卸载 HomeworkChecker。$\r$\n$\r$\n点击$\"卸载$\"继续。"
LangString STR_UNCONFIRM_TOP 1028 "即將從您的電腦解除安裝 HomeworkChecker。$\r$\n$\r$\n按一下「解除安裝」繼續。"
LangString STR_UNCONFIRM_TOP 1033 "The program will be uninstalled from your computer.$\r$\n$\r$\nClick $\"Uninstall$\" to continue."

; --- 安装模式页面 ---
LangString STR_INSTALLMODE_TITLE 2052 "选择安装模式"
LangString STR_INSTALLMODE_TITLE 1028 "選擇安裝模式"
LangString STR_INSTALLMODE_TITLE 1033 "Choose Installation Mode"

LangString STR_INSTALLMODE_DESC 2052 "请选择程序的安装方式。根据您的选择，程序将安装到不同的位置。"
LangString STR_INSTALLMODE_DESC 1028 "請選擇程式的安裝方式。根據您的選擇，程式將安裝到不同的位置。"
LangString STR_INSTALLMODE_DESC 1033 "Choose how the program should be installed. The program will be installed to different locations based on your selection."

LangString STR_INSTALLMODE_ALLUSERS 2052 "为所有用户安装（推荐）"
LangString STR_INSTALLMODE_ALLUSERS 1028 "為所有使用者安裝（推薦）"
LangString STR_INSTALLMODE_ALLUSERS 1033 "Install for all users (Recommended)"

LangString STR_INSTALLMODE_ALLUSERS_DESC 2052 "将程序安装到 Program Files 目录下，所有 Windows 用户账号均可使用此程序。安装时需要管理员权限，安装程序会弹出用户账户控制（UAC）提示。"
LangString STR_INSTALLMODE_ALLUSERS_DESC 1028 "將程式安裝到 Program Files 目錄下，所有 Windows 使用者帳號均可使用此程式。安裝時需要管理員權限，安裝程式會彈出使用者帳戶控制（UAC）提示。"
LangString STR_INSTALLMODE_ALLUSERS_DESC 1033 "Install the program to the Program Files directory. All users on this computer can use the program. Administrator privileges are required and a UAC prompt will appear."

LangString STR_INSTALLMODE_CURRENTUSER 2052 "仅为当前用户安装"
LangString STR_INSTALLMODE_CURRENTUSER 1028 "僅為目前使用者安裝"
LangString STR_INSTALLMODE_CURRENTUSER 1033 "Install for current user only"

LangString STR_INSTALLMODE_CURRENTUSER_DESC 2052 "将程序安装到当前用户的 AppData 目录下，仅当前用户可使用此程序。安装时无需管理员权限。"
LangString STR_INSTALLMODE_CURRENTUSER_DESC 1028 "將程式安裝到目前使用者的 AppData 目錄下，僅目前使用者可使用此程式。安裝時無需管理員權限。"
LangString STR_INSTALLMODE_CURRENTUSER_DESC 1033 "Install the program to the current user's AppData directory. Only the current user can use the program. No administrator privileges required."

LangString STR_INSTALLMODE_TIP 2052 "如果您不确定，请选择$\"为所有用户安装（推荐）$\"。"
LangString STR_INSTALLMODE_TIP 1028 "如果您不確定，請選擇「為所有使用者安裝（推薦）」。"
LangString STR_INSTALLMODE_TIP 1033 "If you are unsure, choose $\"Install for all users (Recommended)$\"."

; --- 快捷方式页面 ---
LangString STR_SHORTCUTS_TITLE 2052 "选择快捷方式选项"
LangString STR_SHORTCUTS_TITLE 1028 "選擇捷徑選項"
LangString STR_SHORTCUTS_TITLE 1033 "Choose Shortcut Options"

LangString STR_SHORTCUTS_DESC 2052 "选择是否创建程序快捷方式，方便您启动程序。"
LangString STR_SHORTCUTS_DESC 1028 "選擇是否建立程式捷徑，方便您啟動程式。"
LangString STR_SHORTCUTS_DESC 1033 "Choose whether to create program shortcuts for easy access."

LangString STR_SHORTCUTS_DESKTOP 2052 "创建桌面快捷方式"
LangString STR_SHORTCUTS_DESKTOP 1028 "建立桌面捷徑"
LangString STR_SHORTCUTS_DESKTOP 1033 "Create desktop shortcut"

LangString STR_SHORTCUTS_DESKTOP_DESC 2052 "在桌面上创建程序快捷方式，双击即可启动 HomeworkChecker。"
LangString STR_SHORTCUTS_DESKTOP_DESC 1028 "在桌面上建立程式捷徑，按兩下即可啟動 HomeworkChecker。"
LangString STR_SHORTCUTS_DESKTOP_DESC 1033 "Create a shortcut on the desktop to launch HomeworkChecker."

LangString STR_SHORTCUTS_STARTMENU 2052 "创建开始菜单快捷方式"
LangString STR_SHORTCUTS_STARTMENU 1028 "建立開始功能表捷徑"
LangString STR_SHORTCUTS_STARTMENU 1033 "Create Start Menu shortcut"

LangString STR_SHORTCUTS_STARTMENU_DESC 2052 "在开始菜单中创建程序快捷方式文件夹，方便从开始菜单查找和启动程序。"
LangString STR_SHORTCUTS_STARTMENU_DESC 1028 "在開始功能表中建立程式捷徑資料夾，方便從開始功能表尋找和啟動程式。"
LangString STR_SHORTCUTS_STARTMENU_DESC 1033 "Create a shortcut folder in the Start Menu for easy access to the program."

; ========== MUI 设置 ==========

!define MUI_ABORTWARNING

!define MUI_WELCOMEPAGE_TITLE "$(STR_WELCOME_TITLE)"
!define MUI_WELCOMEPAGE_TEXT "$(STR_WELCOME_TEXT)"

!define MUI_LICENSEPAGE_TEXT_TOP "$(STR_LICENSE_TOP)"
!define MUI_LICENSEPAGE_CHECKBOX
!define MUI_LICENSEPAGE_CHECKBOX_TEXT "$(STR_LICENSE_CHECKBOX)"

!define MUI_DIRECTORYPAGE_TEXT_TOP "$(STR_DIRECTORY_TOP)"

!define MUI_FINISHPAGE_TITLE "$(STR_FINISH_TITLE)"
!define MUI_FINISHPAGE_TEXT "$(STR_FINISH_TEXT)"

!define MUI_UNCONFIRMPAGE_TEXT_TOP "$(STR_UNCONFIRM_TOP)"

; ========== 语言选择 ==========

!define MUI_LANGDLL_REGISTRY_ROOT "HKCU"
!define MUI_LANGDLL_REGISTRY_KEY "Software\${PRODUCT_NAME}"
!define MUI_LANGDLL_REGISTRY_VALU "InstallerLanguage"
!define MUI_LANGDLL_ALWAYSSHOW

; ========== 页面 ==========

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "InstallFiles\LICENSE"
Page custom InstallModePage_Create InstallModePage_Leave
!define MUI_PAGE_CUSTOMFUNCTION_PRE DirectoryPre
!insertmacro MUI_PAGE_DIRECTORY
Page custom ShortcutsPage_Create ShortcutsPage_Leave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; ========== 卸载页面 ==========

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; ========== 语言 ==========

!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "TradChinese"
!insertmacro MUI_LANGUAGE "English"

; ========== 安装节 ==========

Section "MainInstall" SEC_MAIN
    SectionIn RO

    SetOutPath "$INSTDIR"
    File /r "InstallFiles\*.*"

    ; 创建桌面快捷方式
    ${If} $CreateDesktopShortcut == 1
        CreateShortCut "$DESKTOP\HomeworkChecker.lnk" "$INSTDIR\HomeworkCheckerLauncher.vbs" "" "$INSTDIR\logo.ico"
    ${EndIf}

    ; 创建开始菜单快捷方式
    ${If} $CreateStartMenuShortcut == 1
        CreateDirectory "$SMPROGRAMS\HomeworkChecker"
        CreateShortCut "$SMPROGRAMS\HomeworkChecker\HomeworkChecker.lnk" "$INSTDIR\HomeworkCheckerLauncher.vbs" "" "$INSTDIR\logo.ico"
    ${EndIf}

    ; 写入卸载程序
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    ; 注册 Windows 添加/删除程序
    ${If} $InstallMode == 0
        WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayName" "HomeworkChecker"
        WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "UninstallString" '"$INSTDIR\Uninstall.exe"'
        WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayIcon" "$INSTDIR\logo.ico"
        WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayVersion" "1.7.0-Beta"
        WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "Publisher" "XFTY"
        WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "NoModify" 1
    ${Else}
        WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayName" "HomeworkChecker"
        WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "UninstallString" '"$INSTDIR\Uninstall.exe"'
        WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayIcon" "$INSTDIR\logo.ico"
        WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "DisplayVersion" "1.7.0-Beta"
        WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "Publisher" "XFTY"
        WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "NoModify" 1
    ${EndIf}
SectionEnd

; ========== 自定义页面：安装模式 ==========

Function InstallModePage_Create
    ${If} $SkipCustomPages = 1
        Abort
    ${EndIf}

    nsDialogs::Create 1018
    Pop $0

    ${NSD_CreateLabel} 0u 0u 280u 20u "$(STR_INSTALLMODE_TITLE)"
    Pop $0

    ${NSD_CreateLabel} 0u 22u 280u 24u "$(STR_INSTALLMODE_DESC)"
    Pop $0

    ; Radio: 为所有用户安装
    ${NSD_CreateRadioButton} 10u 54u 260u 12u "$(STR_INSTALLMODE_ALLUSERS)"
    Pop $RadioAllUsers

    ${NSD_CreateLabel} 22u 70u 248u 36u "$(STR_INSTALLMODE_ALLUSERS_DESC)"
    Pop $0

    ; Radio: 仅为当前用户安装
    ${NSD_CreateRadioButton} 10u 116u 260u 12u "$(STR_INSTALLMODE_CURRENTUSER)"
    Pop $RadioCurrentUser

    ${NSD_CreateLabel} 22u 132u 248u 36u "$(STR_INSTALLMODE_CURRENTUSER_DESC)"
    Pop $0

    ${NSD_CreateLabel} 10u 180u 260u 20u "$(STR_INSTALLMODE_TIP)"
    Pop $0

    ; 默认选中"为所有用户安装"
    ${NSD_Check} $RadioAllUsers
    StrCpy $InstallMode 0

    nsDialogs::Show
FunctionEnd

Function InstallModePage_Leave
    ${NSD_GetState} $RadioAllUsers $0
    ${If} $0 == ${BST_CHECKED}
        StrCpy $InstallMode 0
        StrCpy $INSTDIR "$PROGRAMFILES64\HomeworkChecker"
    ${Else}
        StrCpy $InstallMode 1
        StrCpy $INSTDIR "$LOCALAPPDATA\HomeworkChecker"
    ${EndIf}
FunctionEnd

; ========== 自定义页面：快捷方式 ==========

Function ShortcutsPage_Create
    ${If} $SkipCustomPages = 1
        StrCpy $CreateDesktopShortcut 1
        StrCpy $CreateStartMenuShortcut 1
        Abort
    ${EndIf}

    nsDialogs::Create 1018
    Pop $0

    ${NSD_CreateLabel} 0u 0u 280u 20u "$(STR_SHORTCUTS_TITLE)"
    Pop $0

    ${NSD_CreateLabel} 0u 22u 280u 20u "$(STR_SHORTCUTS_DESC)"
    Pop $0

    ; 桌面快捷方式
    ${NSD_CreateCheckBox} 10u 52u 260u 12u "$(STR_SHORTCUTS_DESKTOP)"
    Pop $ChkDesktop

    ${NSD_CreateLabel} 22u 68u 248u 24u "$(STR_SHORTCUTS_DESKTOP_DESC)"
    Pop $0
    ${NSD_Check} $ChkDesktop

    ; 开始菜单快捷方式
    ${NSD_CreateCheckBox} 10u 104u 260u 12u "$(STR_SHORTCUTS_STARTMENU)"
    Pop $ChkStartMenu

    ${NSD_CreateLabel} 22u 120u 248u 24u "$(STR_SHORTCUTS_STARTMENU_DESC)"
    Pop $0
    ${NSD_Check} $ChkStartMenu

    nsDialogs::Show
FunctionEnd

Function ShortcutsPage_Leave
    ${NSD_GetState} $ChkDesktop $CreateDesktopShortcut
    ${NSD_GetState} $ChkStartMenu $CreateStartMenuShortcut
FunctionEnd

; ========== 初始化函数 ==========

Function DirectoryPre
    ${If} $SkipCustomPages = 1
        Abort
    ${EndIf}
FunctionEnd

Function .onInit
    !insertmacro MUI_LANGDLL_DISPLAY

    ; 检测已有安装，跳过安装模式/目录/快捷方式页面
    ReadRegStr $0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "UninstallString"
    ${If} $0 != ""
        StrCpy $SkipCustomPages 1
        StrCpy $InstallMode 0
        StrCpy $CreateDesktopShortcut 1
        StrCpy $CreateStartMenuShortcut 1
    ${Else}
        ReadRegStr $0 HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "UninstallString"
        ${If} $0 != ""
            StrCpy $SkipCustomPages 1
            StrCpy $InstallMode 1
            ReadRegStr $INSTDIR HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker" "InstallLocation"
            StrCpy $CreateDesktopShortcut 1
            StrCpy $CreateStartMenuShortcut 1
        ${EndIf}
    ${EndIf}
FunctionEnd

Function un.onInit
    !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

; ========== 卸载节 ==========

Section "Uninstall"
    ; 删除安装目录
    RMDir /r "$INSTDIR"

    ; 删除桌面快捷方式
    Delete "$DESKTOP\HomeworkChecker.lnk"

    ; 删除开始菜单快捷方式
    RMDir /r "$SMPROGRAMS\HomeworkChecker"

    ; 移除注册表项
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker"
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\HomeworkChecker"
SectionEnd
