; NSIS script for Chunky
; Copyright (c) 2013, Jesper �qvist <jesper@llbit.se>

; Use Modern UI
!include "MUI2.nsh"

Name "Chunky"
OutFile "Chunky.exe"

InstallDir $PROGRAMFILES\Chunky

; Registry key to save install directory
InstallDirRegKey HKLM "Software\Chunky" "Install_Dir"

; request admin privileges
RequestExecutionLevel admin

; Warn on abort
!define MUI_ABORTWARNING

; Pages
!insertmacro MUI_PAGE_LICENSE ../license/LICENSE.txt
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Language
!insertmacro MUI_LANGUAGE "English"

Function .onInit

	Var /GLOBAL JavaExe
	
	; Check for available JRE
	SetRegView 64
	ReadRegStr $0 HKLM "Software\JavaSoft\Java Runtime Environment" "CurrentVersion"
	StrCmp $0 "" FindJDK64
	ReadRegStr $1 HKLM "Software\JavaSoft\Java Runtime Environment\$0" "JavaHome"
	StrCmp $1 "" FindJDK64 FoundJava
	
	FindJDK64:
	ReadRegStr $0 HKLM "Software\JavaSoft\Java Development Kit" "CurrentVersion"
	StrCmp $0 "" FindJRE32
	ReadRegStr $1 HKLM "Software\JavaSoft\Java Development Kit\$0" "JavaHome"
	StrCmp $1 "" FindJRE32 FoundJava
	
	FindJRE32:
	SetRegView 32
	ReadRegStr $0 HKLM "Software\JavaSoft\Java Runtime Environment" "CurrentVersion"
	StrCmp $0 "" FindJDK32
	ReadRegStr $1 HKLM "Software\JavaSoft\Java Runtime Environment\$0" "JavaHome"
	StrCmp $1 "" FindJDK32 FoundJava
	
	FindJDK32:
	ReadRegStr $0 HKLM "Software\JavaSoft\Java Development Kit" "CurrentVersion"
	StrCmp $0 "" NoJava
	ReadRegStr $1 HKLM "Software\JavaSoft\Java Development Kit\$0" "JavaHome"
	StrCmp $1 "" NoJava FoundJava
	
	NoJava:
	MessageBox MB_OK "Could not find Java runtime environment! Please install Java."
	Abort
	
	FoundJava:
	StrCpy $JavaExe "$1\bin\java.exe"
	
FunctionEnd

Section "Chunky (required)" SecChunky

	SectionIn RO

	; Set destination directory
	SetOutPath $INSTDIR
	
	File ..\build\Chunky.jar
	File ..\README.txt
	File ..\ChangeLog.txt
	File chunky.ico
	
	; Write install dir to registry
	WriteRegStr HKLM "Software\Chunky" "Install_Dir" "$INSTDIR"
	
	; Write Windows uninstall keys
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Chunky" "DisplayName" "Chunky"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Chunky" "UninstallString" '"$INSTDIR\uninstall.exe"'
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Chunky" "NoModify" 1
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Chunky" "NoRepair" 1
	WriteUninstaller "Uninstall.exe"

SectionEnd

Section "Start Menu Shortcuts" SecSM

	CreateDirectory "$SMPROGRAMS\Chunky"
	CreateShortCut "$SMPROGRAMS\Chunky\Chunky (More Memory).lnk" "$JavaExe" " -Xms512m -Xmx2g -jar $\"$INSTDIR\Chunky.jar$\"" "$INSTDIR\chunky.ico"
	CreateShortCut "$SMPROGRAMS\Chunky\Chunky.lnk" "$INSTDIR\Chunky.jar" "" "$INSTDIR\chunky.ico"
	CreateShortCut "$SMPROGRAMS\Chunky\Uninstall.lnk" "$INSTDIR\Uninstall.exe"

SectionEnd

;Descriptions

	;Language strings
	LangString DESC_SecChunky ${LANG_ENGLISH} "Installs Chunky"
	LangString DESC_SecSM ${LANG_ENGLISH} "Adds shortcuts to your start menu"

	;Assign language strings to sections
	!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
		!insertmacro MUI_DESCRIPTION_TEXT ${SecChunky} $(DESC_SecChunky)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecSM} $(DESC_SecSM)
	!insertmacro MUI_FUNCTION_DESCRIPTION_END

; Uninstaller

Section "Uninstall"
  
  ; Delete reg keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Chunky"
  DeleteRegKey HKLM SOFTWARE\Chunky

  ; Delete installed files
  Delete $INSTDIR\Chunky.jar
  Delete $INSTDIR\README.txt
  Delete $INSTDIR\LICENSE.txt
  Delete $INSTDIR\Uninstall.exe

  ; Delete shortcuts
  Delete "$SMPROGRAMS\Chunky\*.*"

  ; Remove directories used
  RMDir "$SMPROGRAMS\Chunky"
  RMDir "$INSTDIR"

SectionEnd
