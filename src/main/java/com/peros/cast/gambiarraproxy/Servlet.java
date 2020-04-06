package com.peros.cast.gambiarraproxy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;



@WebServlet(
        name = "RedirectionServlet",
        description = "Example Servlet Using Annotations",
        urlPatterns = {"/teste"}
)
public class Servlet extends HttpServlet {

    @Value("${http.proxyHost}")
    private String HOST_PROXY = "127.0.0.1";
    @Value("${http.proxyPort}")
    private Integer PORT_PROXY = 8888;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response);
    }


    private void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse){
        try {

            String destination = getRedirectUrl(servletRequest);
            String method = servletRequest.getMethod();

            System.out.println("recirecionando para " + destination+ "metodo: "+ method);

                Builder requestBuilder = newRequestBuilder(destination);
                requestBuilder =  copyHeaders(servletRequest,requestBuilder);

                if (! "GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    IOUtils.copyLarge(servletRequest.getInputStream(), baos);

                    MediaType mediaType = null;
                    if (servletRequest.getContentType() != null) {
                        mediaType = MediaType.parse(servletRequest.getContentType());
                    }
                    RequestBody requestBody = RequestBody.create(mediaType, baos.toByteArray());
                    requestBuilder = requestBuilder.method(method, requestBody);
                }

                OkHttpClient client = newOkHttpClient();

                okhttp3.OkHttpClient.Builder builder = client.newBuilder();
                builder = builder.sslSocketFactory(SSLHelper.trustAllSslSocketFactory, (X509TrustManager) SSLHelper.trustAllCerts[0]);
                builder = builder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                builder = builder.connectTimeout(30, TimeUnit.SECONDS);
                builder = builder.writeTimeout(30, TimeUnit.SECONDS);
                builder = builder.readTimeout(30, TimeUnit.SECONDS);

            System.out.println("java.proxy.host" + HOST_PROXY);
            System.out.println("java.proxy.port" + PORT_PROXY);



                if(HOST_PROXY != null && PORT_PROXY != null){
                    client = builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HOST_PROXY,PORT_PROXY))).build();
                }else{
                    client = builder.build();
                }

                Response response = client.newCall(requestBuilder.build()).execute();

                servletResponse.setStatus(response.code());
                Headers responseHeaders = response.headers();

                for (String headerName : responseHeaders.names()) {
                    String headerValue = responseHeaders.get(headerName);
                    servletResponse.setHeader(headerName, headerValue);
                }

                InputStream responseStream = response.body().byteStream();

                IOUtils.copyLarge(responseStream, servletResponse.getOutputStream());
                servletResponse.flushBuffer();


                responseStream.close();
                response.close();

        }catch (Exception e){
            e.printStackTrace();
            try {
                servletResponse.sendError(500);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    private Builder copyHeaders(HttpServletRequest servletRequest, Builder requestBuilder) {
        final Enumeration<String> headers = servletRequest.getHeaderNames();

        while (headers.hasMoreElements()) {
            final String header = headers.nextElement();
            final Enumeration<String> values = servletRequest.getHeaders(header);
            while (values.hasMoreElements()) {
                final String value = values.nextElement();

                //tem que ignorar o host para não barrar
                if("Host".equalsIgnoreCase(header)) {
                    continue;
                }
                requestBuilder = requestBuilder.addHeader(header, value);
            }

        }
        return requestBuilder;
    }

    private String getRedirectUrl(HttpServletRequest servletRequest) throws MalformedURLException, URISyntaxException {
        StringBuffer url = servletRequest.getRequestURL();
        URI requestURI = new URI(url.toString());
        Path path = Paths.get(requestURI.getPath());
        int indexOfProxyInPath = getIndexOfProxyInPath(path);
        if (indexOfProxyInPath < 0 || indexOfProxyInPath > path.getNameCount()) {
            throw new MalformedURLException();

        }
        String destination = path.subpath(indexOfProxyInPath + 1, path.getNameCount()).toString();
        String protocol = "http";//servletRequest.getProtocol().;
        return protocol+ "://"+destination;
    }
    private int getIndexOfProxyInPath(Path path) {
        for (int i = 0; i < path.getNameCount(); i++) {
            if ("redirect".equals(path.getName(i).toString())) {
                return i;
            }
        }
        return -1;
    }

    Builder newRequestBuilder(String url){
        return new Request.Builder().url(url);
    }

    OkHttpClient newOkHttpClient() {
        return new OkHttpClient();
    }
}
