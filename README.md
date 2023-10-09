# VVM-翻译助手

( VisualVM 以下简称 VVM )

VVM-翻译助手是一个基于Java的工具，
旨在自动化VisualVM软件的本地化过程。
它可以将JAR文件中的语言属性文件进行翻译，
从而轻松定制软件的用户界面。

## 特点

- 自动将翻译应用于 VVM JAR 文件。
- 创建 VVM 的备份，以便轻松回滚。
- 识别尚未本地化的剩余语言。
- 支持自定义和扩展以支持其他语言。

## 使用方法

### 使用现成的翻译语言

1. 运行`TranslationConfigWindow`类，这是Swing制作的可视化翻译配置界面。
2. 选择你的 VVM 安装路径。
3. 选择要使用的翻译语言。
4. 在翻译前你可以备份一下，防止意外事故。

### 翻译成其他语言

1. 在接下来的操作前都需要设置 VVM 的路径  
   设置 VVM 的路径```setVvmPath(Path.of("D:\\Program Files\\visualvm\\visualvm_217"))```，  
   必要时请创建备份```createBackup()```，可以使用```rollbackFromBackup()```进行恢复。

2. 使用```readAllLanguages()```读取 VVM 所有的语言，  
   这时你将看到文件```lang/allLang.properties```里面是 VVM 的所有语言。

3. 你可以创建一个翻译语言文件在```lang/translated/(LanguageCode).properties```，然后修改它。

4. 当你不确定还有哪些内容没有翻译完时可以使用```findRemainingLang()```，  
   你可以在文件```lang/remainingLang.properties```查看剩下未翻译的内容。
