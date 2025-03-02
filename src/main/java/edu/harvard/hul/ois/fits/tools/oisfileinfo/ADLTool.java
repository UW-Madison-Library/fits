//
// Copyright (c) 2016 by The President and Fellows of Harvard College
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the License at:
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permission and limitations under the License.
//

package edu.harvard.hul.ois.fits.tools.oisfileinfo;

import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import com.therockquarry.aes31.adl.ADL;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses the audio file parsing library therockquarry.jar provided by Dave Ackerman
 * @author spencer
 *
 */
public class ADLTool extends ToolBase {

	private boolean enabled = true;
	private Fits fits;
	private final static Namespace fitsNS = Namespace.getNamespace(Fits.XML_NAMESPACE);
	private final static Namespace xsiNS = Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");
	private static final Logger logger = LoggerFactory.getLogger(ADLTool.class);

	public ADLTool(Fits fits) throws FitsToolException {
		super();
		this.fits = fits;
		info.setName("ADL Tool");
		info.setVersion("0.1");
		info.setDate("10/24/11");
	}

	public ToolOutput extractInfo(File file) throws FitsToolException {
        logger.debug("ADLTool.extractInfo starting on " + file.getName());
		long startTime = System.currentTimeMillis();
		Document doc = createXml(file);
		output = new ToolOutput(this,(Document)doc.clone(),doc, fits);
		duration = System.currentTimeMillis()-startTime;
		runStatus = RunStatus.SUCCESSFUL;
		logger.debug("ADLTool.extractInfo finishing on " + file.getName());
		return output;
	}

	private Document createXml(File file) throws FitsToolException {

		Element root = new Element("fits",fitsNS);
		root.setAttribute(new Attribute("schemaLocation",
										"http://hul.harvard.edu/ois/xml/ns/fits/fits_output " + fits.getExternalOutputSchema(),
										xsiNS));

		if(file.getPath().toLowerCase().endsWith(".adl")) {

			String adlVersion = null;
			String adlCreator = null;
			String adlCreatorVersion = null;

			try	{
				//creating an ADL object should be enough to validate it as ADL
				new ADL(file);

				Scanner scanner = new Scanner(new FileReader(file));
			    try {
			      //first use a Scanner to get each line
			      while ( scanner.hasNextLine() ){
			    	  String line = scanner.nextLine();
			    	  if(line.contains("(VER_ADL_VERSION)")) {
			    		  adlVersion = getLineVal(line);
			    	  }
			    	  else if(line.contains("(VER_CREATOR)")) {
			    		  adlCreator = getLineVal(line);
			    	  }
			    	  else if(line.contains("(VER_CRTR)")) {
			    		  adlCreatorVersion = getLineVal(line);
			    	  }
			      }
			    }
			    finally {
			      //ensure the underlying stream is always closed
			      scanner.close();
			    }
			}
			catch (Exception e)	{
				throw new FitsToolException("Error parsing ADL file", e);
			}

			Element identification = new Element("identification",fitsNS);
			Element identity = new Element("identity",fitsNS);
			identity.setAttribute("format","Audio Decision List");
			identity.setAttribute("mimetype","text/x-adl");
			Element version = new Element("version",fitsNS);
			version.addContent(adlVersion);
			identity.addContent(version);
			//add identity to identification section
			identification.addContent(identity);
			//add identification section to root
			root.addContent(identification);

			Element fileInfo = new Element("fileinfo",fitsNS);
			Element metadata = new Element("metadata",fitsNS);
			Element textMetadata = new Element("text",fitsNS);

			Element markupLanguage = new Element("markupLanguage",fitsNS);
			markupLanguage.addContent("EDML");
			textMetadata.addContent(markupLanguage);

			if(adlVersion!=null) {
				Element markupLanguageVer = new Element("markupLanguageVersion",fitsNS);
				markupLanguageVer.addContent(adlVersion);
				textMetadata.addContent(markupLanguageVer);
			}
			if(adlCreator!=null) {
				Element elem = new Element("creatingApplicationName",fitsNS);
				elem.addContent(adlCreator);
				fileInfo.addContent(elem);
			}

			if(adlCreatorVersion!=null) {
				Element elem = new Element("creatingApplicationVersion",fitsNS);
				elem.addContent(adlCreatorVersion);
				fileInfo.addContent(elem);
			}

			//add file info section to root
			root.addContent(fileInfo);
			//add to metadata section
			metadata.addContent(textMetadata);
			//add metadata section to root
			root.addContent(metadata);
		}

		return new Document(root);


	}

	private String getLineVal(String line) {
		String[] parts = line.split("\t");
		return parts[parts.length-1];
	}


	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		enabled = value;
	}

}
