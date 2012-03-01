package wyclipse;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.JREContainer;

import wyc.builder.WhileyBuilder;
import wyc.lang.WhileyFile;
import wyclipse.builder.IContainerRoot;
import wyclipse.builder.IContainerRoot.IFileEntry;
import wycore.lang.*;
import wycore.util.JarFileRoot;
import wycore.util.StandardBuildRule;
import wycore.util.Trie;
import wyil.Pipeline;
import wyil.lang.WyilFile;

/**
 * <p>
 * A WhileyProject is responsible for managing resources in the system which are
 * directly or indirectly relevant to compiling Whiley files. For example,
 * source folders containing whiley files are (obviously) directly relevant;
 * however, other containers may be relevant (e.g. if they hold jar files on the
 * whileypath).
 * </p>
 * 
 * <p>
 * The key issue is that when a resource changes which is relevant to the
 * builder (e.g. a Whiley file, etc) then we need to update the namespace to
 * reflect that change.
 * </p>
 * 
 * @author David J. Pearce
 * 
 */
public class WhileyProject implements NameSpace {
	
	/**
	 * The roots of all binary entries known to the builder. This includes all
	 * external archives (e.g. jars), as well as the standard library. It also
	 * includes any output directories.
	 */	
	protected final ArrayList<IContainerRoot> binaryRoots;
	
	/**
	 * The roots of all source entries known to the builder. From this pool of
	 * resources, the set of files needing recompilation is determined.
	 */	
	protected final ArrayList<IContainerRoot> sourceRoots;
	
	/**
	 * The delta is a list of entries which require recompilation. As entries
	 * are changed, they may be added to this list (e.g. Whiley). Entries which
	 * depend upon them may also be added. Or, if they represent e.g. binary
	 * dependents (e.g. jar files) then this may force a total recompilation.
	 */
	protected final ArrayList<IFileEntry> delta;
	
	/**
	 * <p>
	 * The whiley builder is responsible for actually compiling whiley files
	 * into binary files (e.g. class files). The builder operates using a given
	 * set of build rules which determine what whiley files are compiled, what
	 * their target types are and where their binaries should be written.
	 * </p>
	 */
	private final WhileyBuilder builder;
	
	/**
	 * The build rules identify how source files are converted into binary
	 * files. In particular, they determine which whiley files are compiled,
	 * what their target types are and where their binaries should be written.
	 */
	protected final ArrayList<BuildRule> rules;
	
	/**
	 * Construct a build manager from a given IJavaProject. This will traverse
	 * the projects raw classpath to identify all classpath entries, such as
	 * source folders and jar files. These will then be managed by this build
	 * manager.
	 * 
	 * @param project
	 */
	public WhileyProject(IWorkspace workspace, IJavaProject project) throws CoreException {
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		
		binaryRoots = new ArrayList<IContainerRoot>();
		sourceRoots = new ArrayList<IContainerRoot>();
		this.delta = new ArrayList<IFileEntry>();		
		
		// ===============================================================
		// First, initialise roots
		// ===============================================================
		
		IContainer defaultOutputDirectory = workspaceRoot.getFolder(project
				.getOutputLocation());
		
		IContainerRoot outputRoot = null;		
		if(defaultOutputDirectory != null) {
			// we have a default output directory, so make sure to include it!
			outputRoot = new IContainerRoot(defaultOutputDirectory, registry);
			binaryRoots.add(outputRoot);
		}
				
		initialise(workspace.getRoot(),project,project.getRawClasspath());
		
		// ===============================================================
		// Second, initialise builder + rules
		// ===============================================================
		Pipeline pipeline = new Pipeline(Pipeline.defaultPipeline);
		WhileyBuilder builder = new WhileyBuilder(this,pipeline);
		Content.Filter<WhileyFile> includes = Content.filter(Trie.fromString("**"),WhileyFile.ContentType);
		StandardBuildRule rule = new StandardBuildRule(builder);
		
		for(Path.Root source : sourceRoots) {	
			if(outputRoot != null) {
				rule.add(source, includes, outputRoot, WyilFile.ContentType);
			} else {
				// default backup
				rule.add(source, includes, source, WyilFile.ContentType);
			}
		}
		
		rules.add(rule);
	}	
	
	private void initialise(IWorkspaceRoot workspaceRoot,
			IJavaProject javaProject, IClasspathEntry[] entries)
			throws CoreException {
		for (IClasspathEntry e : entries) {
			switch (e.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY : {
					IPath location = e.getPath();
					try {
						binaryRoots.add(new JarFileRoot(location.toOSString(),
								registry));
					} catch (IOException ex) {
						// ignore entries which don't exist
					}
					break;
				}
				case IClasspathEntry.CPE_SOURCE : {
					IPath location = e.getPath();
					IFolder folder = workspaceRoot.getFolder(location);
					sourceRoots.add(new IContainerRoot(folder, registry));
					break;
				}
				case IClasspathEntry.CPE_CONTAINER :
					IPath location = e.getPath();
					IClasspathContainer container = JavaCore
							.getClasspathContainer(location, javaProject);
					if (container instanceof JREContainer) {
						// Ignore JRE container
					} else if (container != null) {
						// Now, recursively add paths
						initialise(workspaceRoot, javaProject,
								container.getClasspathEntries());
					}
					break;
			}
		}
	}
	
	// ======================================================================
	// Accessors
	// ======================================================================		
	
	public boolean exists(Path.ID id, Content.Type<?> ct) throws Exception {
		for(int i=0;i!=binaryRoots.size();++i) {
			if(binaryRoots.get(i).exists(id, ct)) {
				return true;
			}
		}
		return false;
	}
	
	public <T> Path.Entry<T> get(Path.ID id, Content.Type<T> ct) throws Exception {
		for(int i=0;i!=binaryRoots.size();++i) {
			Path.Entry<T> e = binaryRoots.get(i).get(id, ct);
			if(e != null) {
				return e;
			}			
		}
		return null;
	}
	
	public <T> ArrayList<Path.Entry<T>> get(Content.Filter<T> filter) throws Exception {
		ArrayList<Path.Entry<T>> r = new ArrayList<Path.Entry<T>>();
		for(int i=0;i!=binaryRoots.size();++i) {
			r.addAll(binaryRoots.get(i).get(filter));
		}
		return r;
	}
	
	public <T> HashSet<Path.ID> match(Content.Filter<T> filter) throws Exception {
		HashSet<Path.ID> r = new HashSet<Path.ID>();
		for(int i=0;i!=binaryRoots.size();++i) {
			r.addAll(binaryRoots.get(i).match(filter));
		}
		return r;
	}
	
	// ======================================================================
	// Mutators
	// ======================================================================		
	
	public void flush() throws Exception {
		for(int i=0;i!=binaryRoots.size();++i) {
			binaryRoots.get(i).flush();
		}
	}
	
	public void refresh() throws Exception {
		for(int i=0;i!=binaryRoots.size();++i) {
			binaryRoots.get(i).refresh();
		}
	}
	
	/**
	 * A resource of some sort has changed, and we need to update the namespace
	 * accordingly. Note that the given resource may not actually be managed by
	 * this namespace manager --- in which case it can be safely ignored.
	 * 
	 * @param delta
	 */
	public void changed(IResource delta) {
		
	}
	
	/**
	 * A resource of some sort has been created, and we need to update the
	 * namespace accordingly. Note that the given resource may not actually be
	 * managed by this namespace manager --- in which case it can be safely
	 * ignored.
	 * 
	 * @param delta
	 */
	public void created(IResource delta) {
		
	}
		
	/**
	 * A resource of some sort has been removed, and we need to update the
	 * namespace accordingly. Note that the given resource may not actually be
	 * managed by this namespace manager --- in which case it can be safely
	 * ignored.
	 * 
	 * @param delta
	 */
	public void removed(IResource delta) {
		
	}
	
	/**
	 * The master project content type registry.
	 */
	public static final Content.Registry registry = new Content.Registry() {
	
		public void associate(Path.Entry e) {
			if(e.suffix().equals("whiley")) {
				e.associate(WhileyFile.ContentType, null);
			} else if(e.suffix().equals("class")) {
				// this could be either a normal JVM class, or a Wyil class. We
				// need to determine which.
				try { 					
					WyilFile c = WyilFile.ContentType.read(e,e.inputStream());
					if(c != null) {
						e.associate(WyilFile.ContentType,c);
					}					
				} catch(Exception ex) {
					// hmmm, not ideal
				}
			} 
		}
		
		public String suffix(Content.Type<?> t) {
			if(t == WhileyFile.ContentType) {
				return "whiley";
			} else if(t == WyilFile.ContentType) {
				return "class";
			} else {
				return "dat";
			}
		}
	};
}
