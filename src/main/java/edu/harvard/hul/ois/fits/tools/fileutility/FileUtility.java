//
// Copyright (c) 2016 by The President and Fellows of Harvard College
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the License at:
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permission and limitations under the License.
//


package edu.harvard.hul.ois.fits.tools.fileutility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolCLIException;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolInfo;
import edu.harvard.hul.ois.fits.tools.ToolOutput;
import edu.harvard.hul.ois.fits.tools.utils.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtility extends ToolBase {

	private final List<String> UNIX_COMMAND = new ArrayList<String>(Arrays.asList("file"));
	private final List<String> FILE_TEST_COMMAND = new ArrayList<String>(Arrays.asList("which", "file"));
	private boolean enabled = true;
    private final Fits fits;

	private static final Logger logger = LoggerFactory.getLogger(FileUtility.class);

    private final static String xslt = Fits.FITS_XML_DIR+"fileutility/fileutility_to_fits.xslt";

	public FileUtility(Fits fits) throws FitsToolException{
		super();
		this.fits = fits;
        logger.debug ("Initializing FileUtility");
		String osName = System.getProperty("os.name");
		info = new ToolInfo();
		String versionOutput = null;
		List<String> infoCommand;
		info.setName("file utility");
		if (testOSForCommand()){
			//use file command in operating system
			infoCommand = new ArrayList<>(UNIX_COMMAND);
            logger.debug("FileUtility will use system command");
		}

		else {
			//Tool cannot be used on this system
		    logger.error("File Utility cannot be used on this system");
			throw new FitsToolException("File Utility cannot be used on this system");
		}
		infoCommand.add("-v");
		versionOutput = CommandLine.exec(infoCommand,null);
		String[] lines = versionOutput.split("\n");
		String firstLine = lines[0];
		String[] nameVersion = firstLine.split("-");
		info.setVersion(nameVersion[nameVersion.length-1].trim());
		info.setNote(lines[1]);

	}

	public ToolOutput extractInfo(File file) throws FitsToolException {
	    logger.debug("FileUtility.extractInfo starting");
		long startTime = System.currentTimeMillis();

		List<String> execCommand = new ArrayList<>(UNIX_COMMAND);
		execCommand.add("-b"); // omit file name in output
		if(info.getVersion().startsWith("5")) {
			execCommand.add("-e"); // exclude specified test
			execCommand.add("cdf"); //  details of Compound Document Files
		}
		execCommand.add(file.getPath());

		String execOut = CommandLine.exec(execCommand,null);
		if(execOut != null && execOut.length() > 0) {
			execOut = execOut.trim();
		}
		else {
			execOut = "";
		}

		execCommand.add(1, "--mime"); // options must come before file path
		String execMimeOut = CommandLine.exec(execCommand,null);
		if(execMimeOut != null && execMimeOut.length() > 0) {
			execMimeOut = execMimeOut.trim();
		}
		else {
			execMimeOut = "";
		}

		String format = null;
		String mime = null;
		String charset = null;
		List<String> linebreaks = new ArrayList<String>();

		//if mime indicates plain text (except if RTF files)
		if(execMimeOut.startsWith("text/plain") && execMimeOut.contains("charset=") && !execOut.contains("Rich Text Format")) {
			//mime = "text/plain";
			mime = execMimeOut.substring(0,execMimeOut.indexOf("; charset="));
			charset = execMimeOut.substring(execMimeOut.indexOf("=")+1);
			charset = charset.toUpperCase();
			/*if(execOut.contains("ASCII text") ||
					execOut.contains("Unicode text, UTF-32") ||
					execOut.contains("UTF-8 Unicode") ||
					execOut.contains("UTF-16 Unicode") ||
					execOut.contains("Non-ISO extended-ASCII text") ||
					execOut.contains("ISO-8859")) {
				format = "Plain text";
			}*/
			format = "Plain text";

			Pattern p = Pattern.compile("(.*) with (.*) line terminators");
			Matcher m = p.matcher(execOut);
			if(m.matches()) {
				String endings = m.group(2);
				String[] breaks = endings.split(",");
				for(String b : breaks) {
					if(b.equals("CRLF")) {
						b = "CR/LF";
					}
					linebreaks.add(b);
				}
			}
		}
		else if (execOut.contains("Rich Text Format") && execMimeOut.contains("charset=")) {
			format = "Rich Text Format (RTF)";
			mime = execMimeOut.substring(0,execMimeOut.indexOf("; charset="));
		}
		else if(execMimeOut.contains("charset=")) {
			format = execOut;
			mime = execMimeOut.substring(0,execMimeOut.indexOf("; charset="));
		}
		//else use output for format
		else {
			format = execOut;
			mime = execMimeOut;
		}

		Document rawOut = createXml(mime,format,charset,linebreaks,execOut+"\n"+execMimeOut);
		Document fitsXml = transform(xslt,rawOut);

		output = new ToolOutput(this,fitsXml,rawOut, fits);

		duration = System.currentTimeMillis()-startTime;
		runStatus = RunStatus.SUCCESSFUL;
        logger.debug("FileUtility.extractInfo finished");
		return output;
	}

	public boolean testOSForCommand() throws FitsToolCLIException {
		String output = CommandLine.exec(FILE_TEST_COMMAND,null);
		if(output == null || output.length() == 0) {
			return false;
		}
		else {
			return true;
		}
	}
	private Document createXml(String mime_s, String format_s, String charset_s, List<String> linebreaks, String rawOutput_s) throws FitsToolException {
		//xml root
		Element root = new Element("fileUtilityOutput");
		//rawoutput
		Element rawOutput = new Element("rawOutput");
		rawOutput.setText(stripNonValidXMLCharacters(rawOutput_s));
		root.addContent(rawOutput);
		//mimetype
		Element mime = new Element("mimetype");
		mime.setText(mime_s);
		root.addContent(mime);
		//format
		Element format = new Element("format");
		format.setText(stripNonValidXMLCharacters(format_s));
		root.addContent(format);
		//charset
		if(charset_s != null) {
			Element charset = new Element("charset");
			charset.setText(charset_s);
			root.addContent(charset);
		}
		if(linebreaks.size() > 0) {
			for(String l : linebreaks) {
				Element linebreak = new Element("linebreak");
				linebreak.setText(l);
				root.addContent(linebreak);
			}
		}
		return new Document(root);

    }
	/*
	public boolean isIdentityKnown(FileIdentity identity) {
		//identity and mimetype must not be null or empty strings for an identity to be "known"
		if(identity == null
				|| identity.getMime() == null
				|| identity.getMime().length() == 0
				|| identity.getFormat() == null
				|| identity.getFormat().length() == 0) {
			return false;
		}
		String format = identity.getFormat();
		String mime = identity.getMime();
		if(format.equals("data") || format.equals("Unknown Binary") || mime.equals("application/octet-stream")) {
			return false;
		}
		else {
			return true;
		}
	}
*/

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		enabled = value;
	}

	 public String stripNonValidXMLCharacters(String in) {
	        StringBuffer out = new StringBuffer(); // Used to hold the output.
	        char current; // Used to reference the current character.

	        if (in == null || ("".equals(in))) return ""; // vacancy test.
	        for (int i = 0; i < in.length(); i++) {
	            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
	            if ((current == 0x9) ||
	                (current == 0xA) ||
	                (current == 0xD) ||
	                ((current >= 0x20) && (current <= 0xD7FF)) ||
	                ((current >= 0xE000) && (current <= 0xFFFD)) ||
	                ((current >= 0x10000) && (current <= 0x10FFFF)))
	                out.append(current);
	        }
	        return out.toString();
	    }
}
