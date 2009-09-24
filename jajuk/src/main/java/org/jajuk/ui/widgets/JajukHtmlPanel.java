/*
 *  Jajuk
 *  Copyright (C) 2007 The Jajuk Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $Revision$
 */

package org.jajuk.ui.widgets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.jajuk.services.core.SessionService;
import org.jajuk.util.Const;
import org.jajuk.util.DownloadManager;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.log.Log;
import org.w3c.dom.Document;
import org.xamjwg.html.HtmlParserContext;
import org.xamjwg.html.gui.HtmlPanel;
import org.xamjwg.html.parser.DocumentBuilderImpl;
import org.xamjwg.html.parser.InputSourceImpl;
import org.xamjwg.html.test.SimpleHtmlParserContext;
import org.xamjwg.html.test.SimpleHtmlRendererContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Type description
 */
public class JajukHtmlPanel extends HtmlPanel {

  /**
   * 
   */
  private static final String COLON = " : ";

  /**
   * 
   */
  private static final String URL_COLON = "URL: ";

  private static final long serialVersionUID = -4033441908072591661L;

  private final SimpleHtmlRendererContext rcontext;

  private final HtmlParserContext context;

  private final DocumentBuilderImpl dbi;

  /**
   * A HTML renderer based on Cobra
   */
  public JajukHtmlPanel() {
    super();
    // Disable Cobra traces
    Logger.getLogger("").setLevel(Level.OFF);
    rcontext = new SimpleHtmlRendererContext(this);
    context = new SimpleHtmlParserContext();
    dbi = new DocumentBuilderImpl(context, rcontext);
  }

  /**
   * Display a wikipedia url
   * 
   * @throws SAXException
   */
  public void setURL(final URL url, final String lang) {

    SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        File page = new File(SessionService.getConfFileByPath(Const.FILE_CACHE).getAbsolutePath()
            + '/' + UtilSystem.getOnlyFile(url.toString() + ".html"));
        try {
          setCursor(UtilGUI.WAIT_CURSOR);

          // first indicate that we are loading a new page
          setLoading(url);

          String sPage = DownloadManager.downloadText(url);
          // Leave if no result
          if (sPage == null) {
            return null;
          }
          // Remove scripting
          int index = -1;
          int lastindex = -1;
          StringBuilder sb = new StringBuilder(sPage);
          // only the part between <!-- start content --> and <!-- end content
          // --> is
          // important to us
          index = sb.indexOf("<!-- start content -->");
          lastindex = sb.indexOf("</body></html>");
          if (index > 0) {
            sb.delete(0, index);
            sb.delete(sb.indexOf("<!-- end content -->") + 20, lastindex);
          }
          sPage = sb.toString();
          // fix internal links
          sPage = sPage.replaceAll("href=\"/", "href=\"http://"
              + lang + ".wikipedia.org/");
          // Display the page
          showPage(sPage, page);
          // Set current url as a tooltip
          setToolTipText(url.toString());
        } catch (IOException e) {
          // report IOException only as warning here as we can expect this to
          // happen frequently with images on the net
          Log.warn("Could not read page: " + url.toString() + " Cache: " + page, e.getMessage());

          try {
            setFailedToLoad(URL_COLON + url + COLON + e.getClass().getSimpleName() + COLON
                + e.getMessage());
          } catch (IOException e1) {
            Log.error(e1);
          } catch (SAXException e1) {
            Log.error(e1);
          }
        } catch (Exception e) {
          Log.error(e);

          try {
            setFailedToLoad(URL_COLON + url + COLON + e.getClass().getSimpleName() + COLON
                + e.getMessage());
          } catch (IOException e1) {
            Log.error(e1);
          } catch (SAXException e1) {
            Log.error(e1);
          }
        } finally {
          // Disable waiting cursor
          setCursor(UtilGUI.DEFAULT_CURSOR);
        }
        return null;
      }

      @Override
      public void done() {
        try {
          get();
        } catch (InterruptedException e) {
          Log.error(e);
        } catch (ExecutionException e) {
          Log.error(e);
        }
      }

    };
    sw.execute();
  }

  /**
   * Display a "nothing found" page
   * 
   * @throws SAXException
   * 
   * @throws Exception
   */
  public void setUnknown() throws IOException, SAXException {
    File page = new File(SessionService.getConfFileByPath(Const.FILE_CACHE).getAbsolutePath() + '/'
        + "noresult.html");
    String sPage = "<html><body><h1>" + Messages.getString("WikipediaView.10")
        + "</h1></body></html>";
    showPage(sPage, page);
  }

  private void setLoading(final URL url) throws IOException, SAXException {
    File page = new File(SessionService.getConfFileByPath(Const.FILE_CACHE).getAbsolutePath() + '/'
        + "loading.html");
    String sPage = "<html><body><h1>" + Messages.getString("WikipediaView.8") + " "
        + url.toString() + "</h1></body></html>";
    showPage(sPage, page);
  }

  private void setFailedToLoad(String msg) throws IOException, SAXException {
    File page = new File(SessionService.getConfFileByPath(Const.FILE_CACHE).getAbsolutePath() + '/'
        + "failed.html");
    String sPage = "<html><body><h1>" + Messages.getString("WikipediaView.9") + "</h1><br>" + msg
        + "</body></html>";
    showPage(sPage, page);
  }

  /**
   * Make the internal operations
   * 
   * @param sPage
   * @param page
   * @throws IOException
   * @throws SAXException
   */
  private void showPage(String sPage, File page) throws IOException, SAXException {
    // Write the page itself
    Writer bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(page), "UTF-8"));
    try {
      bw.write(sPage);
      bw.flush();
    } finally {
      bw.close();
    }
    // A Reader should be created with the correct charset,
    // which may be obtained from the Content-Type header
    // of an HTTP response.
    Reader reader = new InputStreamReader(new FileInputStream(page), "UTF-8");
    try {
      // InputSourceImpl constructor with URI recommended
      // so the renderer can resolve page component URLs.
      InputSource is = new InputSourceImpl(reader, "file://" + page.getAbsolutePath());
      // A documentURI should be provided to resolve relative
      // URIs.
      Document document = dbi.parse(is);

      // Now set document in panel. This is what causes the
      // document to render.
      setDocument(document, rcontext);
    } finally {
      reader.close();
    }
  }

  public void back() {
    rcontext.back();
  }
}
