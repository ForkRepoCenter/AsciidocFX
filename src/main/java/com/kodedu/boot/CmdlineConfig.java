package com.kodedu.boot;

import de.tototec.cmdoption.CmdOption;

import java.util.LinkedList;
import java.util.List;

public class CmdlineConfig {

    @CmdOption(names = {"--help", "-h"}, description = "显示此帮助")
    boolean help = false;

    @CmdOption(args = "FILE", description = "要打开的文件", maxCount = -1)
    final List<String> files = new LinkedList<>();

    @CmdOption(names = {"--workdir", "-w"}, args = "DIRECTORY", description = "生成打开的文件视图时使用的工作目录")
    String workingDirectory = null;

    @CmdOption(names = {"--backend", "-b"}, args = "BACKEND", description = "定义输出格式: pdf, html, docbook, epub")
    String backend = null;

    @CmdOption(names = {"--headless", "-H"}, description = "选择以无头模式启动AsciidocFX")
    boolean headless = false;

    @CmdOption(names = {"--keep-after", "-K"}, description = "转换完成后保持AsciidocFX运行")
    boolean noQuitAfter = false;

    public boolean isCmdStart() {
        return !files.isEmpty();
    }

    public boolean isHeadless() {
        return headless;
    }

    @Override
    public String toString() {
        return "CmdlineConfig{" +
                "help=" + help +
                ", files=" + files +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", backend='" + backend + '\'' +
                ", headless=" + headless +
                ", noQuitAfter=" + noQuitAfter +
                '}';
    }
}
