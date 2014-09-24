package create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MakeChild {

	public static void main(String[] args) throws Exception {
		// TODO 自动生成的方法存根
		File file = new File("settings.gradle");
		InputStream in = new FileInputStream(file);
		byte[] b = new byte[in.available()];
		in.read(b);
		String str = new String(b,"utf-8");
		Pattern compile = Pattern.compile("\"([^\"]+)\"");
		Matcher matcher = compile.matcher(str);
		while(matcher.find()){
			System.out.println(matcher.group(1));
			make(matcher.group(1));
		}
	}
	
	private static void make(String path) throws Exception{
		File dir = new File(path);
		if(!dir.exists()||!dir.isDirectory()){
			dir.mkdir();
			new File(String.format("%s/src/main/java", path)).mkdirs();
			new File(String.format("%s/src/main/resources", path)).mkdirs();
			new File(String.format("%s/src/test/java", path)).mkdirs();
			new File(String.format("%s/src/test/resources", path)).mkdirs();
			FileOutputStream out = new FileOutputStream(String.format("%s/build.gradle", path));
			out.write(new byte[0]);
			out.flush();
			out.close();
		}
	}

}
