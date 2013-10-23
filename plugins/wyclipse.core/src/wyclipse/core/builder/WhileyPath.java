package wyclipse.core.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import wybs.lang.Content;
import wybs.lang.Path;
import wybs.util.Trie;
import wyc.lang.WhileyFile;
import wycs.core.WycsFile;
import wycs.syntax.WyalFile;
import wyil.lang.WyilFile;

/**
 * The <code>whileypath</code> controls the way in which files and folders in a
 * Whiley project are interpreted. In particular, some directories will be
 * identified as source folders, whilst others will be identifier as binary
 * output folders. Likewise, libraries (e.g. jar files) may be specified for
 * linking against.
 * 
 * @author David J. Pearce
 * 
 */
public final class WhileyPath {
	private final ArrayList<Entry> entries;
	private IPath defaultOutputFolder;
	
	public WhileyPath() {
		entries = new ArrayList<Entry>();
	}
	
	public WhileyPath(IPath defaultOutputFolder, Entry... entries) {
		this.defaultOutputFolder = defaultOutputFolder;
		this.entries = new ArrayList<Entry>();
		for(Entry e : entries) {
			this.entries.add(e);
		}
	}
	
	public WhileyPath(IPath defaultOutputFolder, Collection<Entry> entries) {
		this.defaultOutputFolder = defaultOutputFolder;
		this.entries = new ArrayList<Entry>(entries);
	}

	public IPath getDefaultOutputFolder() {
		return defaultOutputFolder;
	}
	
	public void setDefaultOutputFolder(IPath defaultOutputFolder) {
		this.defaultOutputFolder = defaultOutputFolder;
	}
	
	public List<Entry> getEntries() {
		return entries;
	}
	
	public Document toXmlDocument() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement("whileypath");
			if (defaultOutputFolder != null) {
				root.setAttribute("bindir",
						defaultOutputFolder.toString());
			}
			doc.appendChild(root);

			for (Entry e : entries) {
				if (e instanceof BuildRule) {
					BuildRule action = (BuildRule) e;
					Element child = doc.createElement("buildrule");
					// FIXME: need to do more here!
					child.setAttribute("target", "wyil");
					child.setAttribute("srcdir", action.getSourceFolder()
							.toString());
					child.setAttribute("includes", action.getSourceIncludes()
							.toString());
					if (action.getOutputFolder() != null) {
						child.setAttribute("bindir", action.getOutputFolder()
								.toString());
					}
					root.appendChild(child);
				} else if(e instanceof ExternalLibrary) {
					ExternalLibrary el = (ExternalLibrary) e;
					Element child = doc.createElement("library");
					child.setAttribute("path", el.getLocation().toString());
					child.setAttribute("includes", el.getIncludes().toString());
					root.appendChild(child);
				}
			}

			return doc;
		} catch (Exception e) {
			// ?
			return null;
		}
	}
	
	/**
	 * Construct a WhileyPath object from an XML document. If the document is
	 * corrupt or invalid in some way, then this will simply return null.
	 * 
	 * @param xmldoc
	 * @return
	 */
	public static final WhileyPath fromXmlDocument(Document xmldoc) {
		WhileyPath whileypath = new WhileyPath();
		
		// First, check whether or not a bindir attribute is given on the root
		// of the whileypath file.
		Node root = xmldoc.getFirstChild();
		Node globalBinDir = root.getAttributes().getNamedItem("bindir");
		if (globalBinDir != null) {
			// Yup, it exists.
			whileypath
					.setDefaultOutputFolder(new org.eclipse.core.runtime.Path(
							globalBinDir.getNodeValue()));
		}
		
		List<Entry> whileyPathEntries = whileypath.getEntries();
		NodeList children = root.getChildNodes();
		for (int i = 0; i != children.getLength(); ++i) {
			Node child = children.item(i);
			String childName = child.getNodeName();
			if (childName.equals("buildrule")) {
				NamedNodeMap attributes = child.getAttributes();
				IPath sourceFolder = new org.eclipse.core.runtime.Path(
						attributes.getNamedItem("srcdir").getNodeValue());
				Path.Filter sourceIncludes = Trie.fromString(attributes
						.getNamedItem("includes").getNodeValue());
				Node bindir = attributes.getNamedItem("bindir");
				IPath outputFolder = bindir == null ? null
						: new org.eclipse.core.runtime.Path(
								bindir.getNodeValue());
				whileyPathEntries.add(new WhileyPath.BuildRule(sourceFolder,
						sourceIncludes, outputFolder));
			} else if (childName.equals("library")) {
				NamedNodeMap attributes = child.getAttributes();
				IPath location = new org.eclipse.core.runtime.Path(attributes
						.getNamedItem("srcdir").getNodeValue());
				Path.Filter includes = Trie.fromString(attributes.getNamedItem(
						"includes").getNodeValue());
				whileyPathEntries.add(new WhileyPath.ExternalLibrary(location,
						includes));
			}
		}
		
		return whileypath;
	}
	
	/**
	 * Represents an abstract item on the whileypath, which could be a build
	 * rule or a container of some sort.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static abstract class Entry {
		
	}
	
	/**
	 * <p>
	 * Represents an external folder or library on the whilepath which contains
	 * various files needed for compilation. External files are not modified in
	 * any way by the builder.
	 * </p>
	 * <p>
	 * <b>NOTE:</b> currently, external libraries must hold WyIL files.
	 * </p>
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static final class ExternalLibrary extends Entry {
		/**
		 * The location of the folder containing whiley source files. Observe
		 * that this may be relative to the project root, or an absolute
		 * location.
		 */
		private IPath location;
		
		/**
		 * Describes the set of files which are included in this library.
		 */
		private Path.Filter includes;
		
		public ExternalLibrary(IPath location, Path.Filter includes) {	
			this.location = location;
			this.includes = includes;
		}
		
		public IPath getLocation() {
			return location;
		}
		
		public Path.Filter getIncludes() {
			return includes;
		}
	}
	
	/**
	 * Represents an action for compiling Whiley source files to a given target.
	 * An optional output folder may also be supplied. From this action, the
	 * necessary build rules for generating code for the given target can then
	 * be created.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static final class BuildRule extends Entry {
		
		/**
		 * The location of the folder containing whiley source files. Observe
		 * that this may be relative to the project root, or an absolute
		 * location.
		 */
		private IPath sourceFolder;
		
		/**
		 * Describes the set of source files which are included in this action.
		 */
		private Path.Filter sourceIncludes;
		
		/**
		 * The location of the folder where binary (i.e. compiled) files are
		 * placed. Observe that this may be relative to the project root, or an
		 * absolute location. Note also that this is optional, and may be null
		 * (in which case the defaultOutputFolder is used).
		 */
		private IPath outputFolder;
	
		public BuildRule(IPath sourceFolder, Path.Filter sourceIncludes, IPath outputFolder) {
			this.sourceFolder = sourceFolder;
			this.sourceIncludes = sourceIncludes;
			this.outputFolder = outputFolder;
		}	
		
		
		public IPath getSourceFolder() {
			return sourceFolder;
		}
		
		public void setSourceFolder(IPath sourceFolder) {
			this.sourceFolder = sourceFolder;
		}
		
		public Path.Filter getSourceIncludes() {
			return sourceIncludes;
		}
		
		public void setSourceIncludes(Path.Filter sourceIncludes) {
			this.sourceIncludes = sourceIncludes;
		}
		
		public IPath getOutputFolder() {
			return outputFolder;
		}
		
		public void setOutputFolder(IPath outputFolder) {
			this.outputFolder = outputFolder;
		}
	}
}
