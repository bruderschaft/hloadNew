import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
//import org.apache.commons.lang.StringEscapeUtils;


/**
 * Created by yar 09.09.2009
 */
public class HttpServer {

	public static void main(String[] args) throws Throwable {
		WorkQueue workQueue = new WorkQueue(10);
		ServerSocket ss = new ServerSocket(8012);
		while (true) {
			Socket s = ss.accept();
			System.err.println("Client accepted");
			//new Thread(new SocketProcessor(s)).start();
			workQueue.execute(new SocketProcessor(s));
			System.out.println(Thread.activeCount());

		}
	}

	public static class SocketProcessor implements Runnable {

		private Socket s;
		private InputStream is;
		private OutputStream os;

		private SocketProcessor(Socket s) throws Throwable {
			this.s = s;
			this.is = s.getInputStream();
			this.os = s.getOutputStream();
		}

		public void run() {
			try {
				String[] parameters = readInputHeaders();
                if (!parameters[0].equals("GET") && !parameters[0].equals("HEAD")){
                    writeHeaders(405, "", "", 0);
                } if (parameters[1].contains("../")){
                    writeHeaders(403, "", "", 0);
                } else {
                    String path = "/home/max/IdeaProjects/hload/test" + parameters[1];

                    path = URLDecoder.decode(path);

                    if (path.contains("?")){
                        path = path.substring(0,path.indexOf("?"));
                    }
                    System.err.println(path);
                    File file = new File(path);
                    //System.out.println("exist:" + file.exists());
                    if (!file.exists()){
                        writeHeaders(404, "", "", 0);
                    } else {
                        if (path.endsWith("/")){
                            path += "index.html";
                            file = new File(path);

                        }
                        if (!file.exists()){
                            writeHeaders(403, "", "", 0);
                            os.flush();
                        } else {
                            String typeParse[] = path.split("\\.");
                            //ошибочки проверить
                            String type = typeParse[typeParse.length-1];
                            String typeName = "";
                            String processingType = "";
                            if (type.equals("html")){
                                typeName = "text/html";

                            } else if (type.equals("css")){
                                typeName = "text/css";
                            } else if (type.equals("js")){
                                typeName = "text/javascript";
                            } else if (type.equals("jpg") || type.equals("jpeg")){
                                typeName = "image/jpeg";
                            } else if (type.equals("png")){
                                typeName = "image/png";
                            } else if (type.equals("gif")){
                                typeName = "image/gif";
                            } else if (type.equals("swf")){
                                typeName = "application/x-shockwave-flash";
                            } else {
                                typeName = "text";
                            }
                            processingType = typeName.split("/")[0];
                            String data = "";
                            byte[] byteArray = new byte[5147483];
                            System.out.println("filename: " + file.getName());
                            if (processingType.equals("text")){
                                BufferedReader reader = new BufferedReader(new FileReader(path));
                                StringBuffer sb = new StringBuffer();
                                while (true){
                                    String buffer = reader.readLine();
                                    if (buffer == null){
                                        break;
                                    }
                                    sb.append(buffer + "\n");
                                }
                                data = sb.toString();
                                byteArray = data.getBytes();
                            } else {
                                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
                                while ((bis.read(byteArray)) != -1){
                                }
                                bis.close();
                            }

                            //System.out.println("data: " + data);

                            writeHeaders(200, data, typeName, file.length());
                            if (parameters[0].equals("GET")){
                                os.write(byteArray);
                            }
                            os.flush();
                        }

                    }
                }

                //System.out.println("addr: " + new Scanner(is).useDelimiter("\\Z").next());
				//writeResponse("<html><body><h1>Hello from Habrahabr</h1></body></html>");

			} catch (Throwable t) {
                /*do nothing*/
			} finally {
				try {
					s.close();
				} catch (Throwable t) {
                    /*do nothing*/
				}
			}
			System.err.println("Client processing finished");
		}

		public void writeHeaders(int code, String data, String type, long length) throws Throwable{
			String result = "HTTP/1.1 ";
			switch (code){
				case 200: result += "200 OK"; break;
				case 404: result += "404 Not Found"; break;
				case 405: result += "405 Method Not Allowed"; break;
                case 403: result += "403 Forbidden";
			}
			result += "\r\n";
			result += "Date: ";
			Date date = new Date();
			DateFormat dateFormat =
					new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			result += dateFormat.format(date) + "\r\n";
            result += "Server: myServer\r\n";
            result += "Content-Length: " + length + "\r\n";
            System.out.println("typpe: " + type);
            result += "Content-Type: " + type + "\r\n";

            result += "Connection: close\r\n\r\n";

			os.write(result.getBytes());
		}

        public void writeAnswer(byte[] bytes) throws Throwable{
            os.write(bytes);
        }

		private void writeResponse(String s) throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" +
					"Server: YarServer/2009-09-09\r\n" +
					"Content-Type: text/html\r\n" +
					"Content-Length: " + s.length() + "\r\n" +
					"Connection: close\r\n\r\n";
			String result = response + s;
			os.write(result.getBytes());
			os.flush();
		}

		private String[] readInputHeaders() throws Throwable {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String s = br.readLine();
            String[] parameters = s.split(" ");
            if (parameters.length > 3){
                parameters[0] = parameters[0];
                for(int i=2; i<parameters.length-1; i++){
                    parameters[1] += " " + parameters[i];
                }
            }
            for (int i = 0; i<parameters.length; i++){
                System.out.println(parameters[i]);
            }
            while(true) {
				s = br.readLine();
                System.out.println(s);
                if(s == null || s.trim().length() == 0) {
                    break;
				}
			}
            return parameters;
		}
	}
}