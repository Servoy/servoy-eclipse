package com.servoy.eclipse.designer.rfb.startup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Utils;

@WebServlet("/rfb/*")
public class ResourcesServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String path = req.getPathInfo();
		if ("/".equals(path)) path = "/index.html";
		path = Paths.get(req.getServletPath() + path).normalize().toString().replace('\\', '/');
		if (path.startsWith("/rfb/"))
		{
			URL res = getClass().getResource(path);
			if (res != null)
			{
				URLConnection uc = res.openConnection();
				uc.setUseCaches(false);
				if (HTTPUtils.checkAndSetUnmodified(req, resp, uc.getLastModified())) return;
				resp.setContentLength(uc.getContentLength());
				resp.setContentType(MimeTypes.guessContentTypeFromName(path));
				InputStream in = uc.getInputStream();
				ServletOutputStream outputStream = resp.getOutputStream();
				Utils.streamCopy(in, outputStream);
				outputStream.flush();
				Utils.close(in);
			}
			else
			{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}
}
