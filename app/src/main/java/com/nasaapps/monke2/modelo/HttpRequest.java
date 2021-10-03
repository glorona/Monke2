package com.nasaapps.monke2.modelo;

import android.os.StrictMode;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

public class HttpRequest {
    private final URL url;
    private PrintWriter writer;
    private String rawString;
    private boolean multipart;
    private HttpURLConnection http;
    private HttpsURLConnection https;
    private OutputStream output;
    private final String method;
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    public static final String POST_METHOD = "POST";
    public static final String GET_METHOD = "GET";
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private String queryString;
    private static StrictMode.ThreadPolicy p = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    public HttpRequest(String url, boolean multipart, String method) throws IOException
    {
        StrictMode.setThreadPolicy(p);
        this.url = new URL(url);
        this.multipart = multipart;
        this.method = method;
        this.queryString = null;
        if (multipart)
            boundary = generateBoundary();
        else
            this.boundary = null;
        openConnection("");
    }
    private boolean isHttp()
    {
        return this.url.getProtocol().equals(HTTP);
    }
    private String generateBoundary()
    {
        return "===" + System.currentTimeMillis() + "===";
    }

    private void openConnection(String Credentials) throws IOException
    {
        StrictMode.setThreadPolicy(p);
        if (isHttp())
        {
            http = (HttpURLConnection) this.url.openConnection();
            http.setUseCaches(false);
            http.setRequestMethod(method);
            http.setDoInput(true);
            http.setConnectTimeout(30000);
            http.setReadTimeout(30000);
            //http.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(Credentials.getBytes(), Base64.NO_WRAP));
            http.setRequestProperty("Accept","application/json");
            if(rawString != null)
                http.setRequestProperty("Content-Type","application/json");

            if (multipart)
            {
                http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            }
            if(method.equals(POST_METHOD)) {
                http.setDoOutput(true);
                output = http.getOutputStream();
            }
        }
        else
        {
            https = (HttpsURLConnection) this.url.openConnection();
            https.setRequestMethod(method);
            https.setUseCaches(false);
            https.setDoInput(true);
            https.setConnectTimeout(30000);
            https.setReadTimeout(30000);
            //http.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(Credentials.getBytes(), Base64.NO_WRAP));
            if (multipart)
            {
                https.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            }
            if(method.equals(POST_METHOD)) {
                https.setDoOutput(true);
                output = https.getOutputStream();
            }
        }
        if(method.equals(POST_METHOD)) {
            writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
        }

    }
    private String readInput(InputStream input) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        try  {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while((line = reader.readLine()) != null)
                builder.append(line);
        }catch (Exception ignored){}
        return builder.toString();
    }
    public HttpResponse execute() throws IOException
    {
        StrictMode.setThreadPolicy(p);
        HttpResponse _response = new HttpResponse();
        if (multipart)
        {
            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
        }
        else if (queryString != null)
        {
            writer.print(queryString);
            writer.flush();
        }
        else if (rawString != null)
        {
            writer.println(rawString);
            writer.flush();
        }
        if(method.equals(POST_METHOD))
            writer.close();

        _response.setStatusCode((isHttp())?http.getResponseCode():https.getResponseCode());
        switch (_response.getStatusCode())
        {
            case 200:
            case 201:/****EXITO***/
                _response.setBody(readInput((isHttp())?http.getInputStream():https.getInputStream()));
                break;
            case 422:/******ERROR DE PARAMETROS FALTANTES O INVALIDOS*****/
                _response.setBody(readInput((isHttp())?http.getErrorStream():https.getErrorStream()));
                _response.setError(true);
                break;
            case 403:/***** CONTRASEÃ‘A INCORRECTA *****/
                _response.setBody(readInput((isHttp())?http.getErrorStream():https.getErrorStream()));
                _response.setError(true);
                break;
            case 404:/******NO SE ENCONTRO EL RECURSO***/
                _response.setBody(readInput((isHttp())?http.getErrorStream():https.getErrorStream()));
                _response.setError(true);
                break;
            case 500:/********ERROR SERVIDOR*****/
                _response.setBody(readInput((isHttp())?http.getErrorStream():https.getErrorStream()));
                _response.setError(true);
                break;
            default:
                _response.setBody(readInput((isHttp())?http.getErrorStream():https.getErrorStream()));
                _response.setError(true);
                break;
        }
        return _response;
    }
    public HttpRequest(String url, String rawString, String method) throws IOException
    {
        this.url = new URL(url);
        this.boundary = null;
        this.rawString = rawString;
        this.multipart = false;
        this.method = method;
        this.queryString = null;
        openConnection("");
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    public String getRawString() {
        return rawString;
    }

    public void setRawString(String rawString) {
        this.rawString = rawString;
    }
    public void appendRequestProperty(String property, String value)
    {
        if (isHttp())
            http.setRequestProperty(property, value);
        else
            https.setRequestProperty(property, value);
    }
    public void appendFile(String param, File file) throws IOException
    {
        if (multipart)
        {
            String fileName = file.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + param
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
            try  {
                FileInputStream inputStream = new FileInputStream(file);
                byte[] buffer = new byte[inputStream.available()];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            }catch (Exception ex){
                String e=ex.getMessage();
            }
            writer.append(LINE_FEED);
            writer.flush();
        }
    }
    public void appendFormData(String param, String value)
    {
        if (multipart)
        {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + param + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }
        else
        {
            if (queryString == null)
                queryString = param + "=" + value;
            else
                queryString+= "&" + param + "=" + value;
        }

    }
}
