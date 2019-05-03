//revo uninstaller
//
//dpkg remove

package model;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.BsuirException;

/**
 * This class represents http-request model (�����, ��������������� ������ http
 * �������).
 *
 * @author Yuzvenko Polina and Konevega Evgeny
 */
public class HttpRequest {
	/**
	 * Logger for logging.
	 */
	private static Logger lOGGER = LogManager.getLogger(HttpRequest.class.getName());
	/**
	 * Default request value.
	 */
	private String request = "";
	/**
	 * List for request parameters.
	 **/
	private List<Field> fields = new ArrayList<>();
	private String Method = "";
	private String URL = "";
	private String Host = "";
	private String Referer = "";
	private String ContentLength = "";
	private String AcceptEncoding = "gzip, deflate";
	private String AcceptLanguage = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7";
	private String Accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
	private String ContentType = "application/x-www-form-urlencoded";
	private String Connection = "Keep-Alive";
	private String Port = "8080";
	private String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36";
	/**
	 * Map for request parameters.
	 **/
	private Map<String, String> requestFields = createMap();

	private static Map<String, String> createMap() {
		Map<String, String> requestFields = new HashMap<String, String>();
		return requestFields;
	}

	/**
	 * Public model constructor.
	 */
	public HttpRequest() {
		for (Field field : HttpRequest.class.getDeclaredFields()) {
			if (field.getType() == String.class && field.getModifiers() == Modifier.PRIVATE) {
				field.setAccessible(true);
				fields.add(field);
			}
		}
	}

	// 127.0.0.1:8080/testServer/welcome1
	private void getAndHead(String text) {
		Port = Utils.findPort(text);
		Referer = text;
		if(text.contains("/")) {
		URL = text.substring(text.indexOf("/"), text.length()); // for 127.0.0.1:8080/testServer/welcome1
		} else { URL = "/"; }
		if (text.contains(":")) {
			Host = text.substring(0, text.indexOf(":"));
		} else {
			Host = text.substring(0, text.indexOf("/"));
		}
	}

	/**
	 * Method set new http-request text.
	 *
	 * @param newRequest
	 * @param method
	 * @throws BsuirException new users exception
	 */
	public void setNewRequest(String newRequest, String method) throws BsuirException {
		try {
			String[] request = newRequest.split("\n");
			Method = method;
			Referer = request[0];
			getAndHead(request[0]);

			if (method == "POST") {
				int len = 0;
				for (int index = 1; index < request.length; index++) {
					if (request[index].split(":")[0].equals("Params")) {
						index++;
						while (!request[index].equals("}")) {
							String[] params = request[index].split("\\s*(\\s|,|!|\\.)\\s*");
							index++;
							requestFields.put("Param:" + params[1], params[3].substring(0, params[3].length() - 1));
							len += (params[1].length() + params[3].length());

						}
					}
				}
				ContentLength = String.valueOf(len + 1);
			}

		} catch (IllegalArgumentException e) {
			lOGGER.debug("IllegalArgumentException in setNewRequest function");
			throw new BsuirException("IllegalArgumentException in setNewRequest function");
		}
	}

	/**
	 * Method forms new htto-request text for socket.
	 *
	 * @param text - text, which enter user
	 * @return new request
	 * @throws BsuirException user exception
	 */
	public String request(String text, String method) throws BsuirException {
		setNewRequest(text, method);
		List<String> keys = new ArrayList<String>(requestFields.keySet());
		ArrayList<String> params = new ArrayList<String>();
		ArrayList<String> paramValues = new ArrayList<String>();

		if (Method == "GET") {
			request = setGetRequest();
		} else if (Method == "HEAD") {
			request = setHeadRequest();
		} else {
			request = setPostRequest(keys, params, paramValues);
		}

		return request;
	}

	private String setGetRequest() {
		request = Method + " " + URL + " HTTP/1.1\r\n";
		request += "Host: " + Host + ":" + Port + "\r\n";
		request += "Referer: " + Referer + "\r\n";
		request += "Accept: " + Accept + "\r\n";
		request += "Accept-Encoding: " + AcceptEncoding + "\r\n";
		request += "Accept-Language: " + AcceptLanguage + "\r\n";
		request += "Connection: " + Connection + "\r\n";
		request += "User-Agent: " + UserAgent + "\r\n";

		return request;
	}

	private String setHeadRequest() {
		request = Method + " " + URL + " HTTP/1.1\r\n";
		request += "Host: " + Host + ":" + Port + "\r\n";
		request += "Referer: " + Referer + "\r\n";
		request += "Accept: " + Accept + "\r\n";
		request += "Accept-Encoding: " + AcceptEncoding + "\r\n";
		request += "Accept-Language: " + AcceptLanguage + "\r\n";
		request += "Connection: " + Connection + "\r\n";
		request += "User-Agent: " + UserAgent + "\r\n";

		return request;
	}

	private String setPostRequest(List<String> keys, ArrayList<String> params, ArrayList<String> paramValues) {
		request = Method + " " + URL + " HTTP/1.1\r\n";
		request += "Host: " + Host + ":" + Port + "\r\n";
		request += "Content-Length: " + ContentLength + "\r\n";
		request += "Content-Type: " + ContentType + "\r\n";
		request += "Connection: " + Connection + "\r\n";

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			if (key.split(":")[0].equals("Param")) {
				params.add(key.split(":")[1]);
				paramValues.add(requestFields.get(key));
			}
		}

		for (int i = 0; i < params.size(); i++) {
			if (i == 0) {
				try {
					request += "\r\n" + URLEncoder.encode(params.get(i), "UTF-8") + "="
							+ URLEncoder.encode(paramValues.get(i), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				try {
					request += "&" + URLEncoder.encode(params.get(i), "UTF-8") + "="
							+ URLEncoder.encode(paramValues.get(i), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		return request;
	}

	public String testFunc(String text, HttpRequest httpRequest)
			throws IllegalArgumentException, IllegalAccessException {
		String result = "";
		for (int index = 0; index < fields.size(); index++) {
			if (fields.get(index).getName().equals(text)) {
				result = (String) fields.get(index).get(httpRequest);
			}
		}
		return result;
	}

	public String getPort() {
		return Port;
	}

	public String getHost() {
		return requestFields.get("Host");
	}
}