package com.essabir.exam.utlis;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends Request<String> {

    private static final String BOUNDARY = "your_boundary_here";
    private static final String LINE_FEED = "\r\n";
    private Map<String, String> headers;
    private final Response.Listener<String> mListener;
    private Map<String, String> params;
    private Bitmap bitmap;
    private String fileName;
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public MultipartRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener,
                            Bitmap bitmap, String fileName, Map<String, String> params) {
        super(method, url, errorListener);
        this.headers = new HashMap<>();
        this.params = params;
        this.bitmap = bitmap;
        this.mListener = listener;
        this.fileName = fileName;

        setShouldCache(false);
        buildMultipartEntity();
    }

    private void buildMultipartEntity() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] data = bos.toByteArray();
        try {
            // Add your parameters
            for (Map.Entry<String, String> entry : params.entrySet()) {
                addPart(entry.getKey(), entry.getValue());
            }

            // Add the image file
            addFilePart("image", data, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPart(String key, String value) throws IOException {
        writeLine("--" + BOUNDARY);
        writeLine("Content-Disposition: form-data; name=\"" + key + "\"");
        writeLine("Content-Type: text/plain; charset=UTF-8");
        writeLine("");
        writeLine(value);
    }

    private void addFilePart(String fieldName, byte[] file, String fileName) throws IOException {
        writeLine("--" + BOUNDARY);
        writeLine("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"");
        writeLine("Content-Type: image/jpeg");
        writeLine("Content-Transfer-Encoding: binary");
        writeLine("");
        bos.write(file);
        writeLine("");
    }

    private void writeLine(String value) throws IOException {
        bos.write(value.getBytes());
        bos.write(LINE_FEED.getBytes());
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + BOUNDARY;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        try {
            buildMultipartEntity();
            bos.write(LINE_FEED.getBytes());
            bos.write(("--" + BOUNDARY + "--").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success("Image Created", null);
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        Log.d("MultipartRequest", "Server Response: " + response.toString());
        mListener.onResponse(response);
    }
}
