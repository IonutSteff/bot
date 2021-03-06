package everyos.discord.bot.util;

import java.io.File;

public class FileUtil {
    private static String dir;
    static String seper = "/";
    static {
        String mdir;
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            mdir =  new File(System.getenv("APPDATA")).getAbsolutePath();
            seper = "\\";
        } else {
            mdir = new File(System.getProperty("user.home")).getAbsolutePath();
        }
        dir = mdir + seper + "everyos" + seper + "bot";
    }

    public static String getAppData(String path){
        return dir+seper+path;
	}

	public static String join(String path, String file) {
		return path+seper+file;
	}
}