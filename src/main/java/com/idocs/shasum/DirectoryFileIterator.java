package com.idocs.shasum;


import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;


/**
 * Iterator of files under a given parent path. Supports descending into
 * sub-directories and patterns that the returned files should match
 * (fileAcceptPattern) or should not match (fileRejectPattern). Only the
 * contents of a single scanned directory are kept in memory, so that this can
 * be used with a long and deep directory structure.
 * 
 * @author a.giotis
 */
public class DirectoryFileIterator implements Iterator<File> {
	
	/*
	 * Configuration properties
	 */
	
	private final String parentDirectoryPath;
	
	private final boolean recursive;
	/** The optional filter to apply to directories */
	private final FileFilter dirFilter;
	
	/** The optional filter to apply to normal files */
	private final FileFilter fileFilter;

	private final Logger log;

	/*
	 * Runtime variables
	 */
	
	private final List<String> dirStack = new ArrayList<String>();

	/** The files of the directory lastly popped from the stack. */
	private File dirFile;
	private String[] dirContents;
	private int dirContentsIndex;
	private File nextFile;

	/*
	 * Counters to log directory scanning statistics.
	 */

	private int matchingFilesCount = 0;
	private long matchingFilesLength = 0;
	private int scannedDirsCount = 0;
	private int scannedFilesCount = 0;
	/**
	 * The subset of the scanned files that were not accepted by the file filter.
	 */
	private int rejectedFilesCount = 0;
	
	/**
	 * The subset of scanned directories that were not accepted by the dir filter.
	 */
	private int rejectedDirsCount = 0;
	
	private final List<String> emptyDirs = new ArrayList<String>();

	/**
	 * Creates an iterator of files in a directory.
	 * 
	 * @param parentDirectoryPath
	 *            The parent directory path.
	 * @param recursive
	 *            , true if we should recurse into sub-directories, false
	 *            otherwise.
	 * @param dirFilter
	 *            , the filter that accepts descending or not in to sub
	 *            directories of any level of the directory path. Set as null,
	 *            to disable this filter.
	 * @param fileFilter
	 *            , the filter to accept or reject the files in any subdirectory
	 *            of the directory path. Set as null, to disable this filter.
	 * 
	 * @param log
	 *            The log to log information. Set as null, to disable
	 *            logging.
	 */
	public DirectoryFileIterator(String parentDirectoryPath, boolean recursive,
			FileFilter dirFilter, FileFilter fileFilter, Logger logger) {
		this.parentDirectoryPath = new File(parentDirectoryPath, "").getAbsolutePath();
		// The File constructor with parent/child supports a null parent.
		this.recursive = recursive;
		this.dirFilter = dirFilter;
		this.fileFilter = fileFilter;
		this.log = logger;
		
		dirStack.add(this.parentDirectoryPath);
		nextFile = findNextMatchingFile();
	}

	@Override
	public void remove() {
		// Could delete the files, but there is no such use case. 
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean hasNext() {
		return nextFile != null;
	}

	@Override
	public File next() {
		if(nextFile == null)
			throw new NoSuchElementException(); // required by the interface
		
		File toReturn = nextFile;
		
		nextFile = findNextMatchingFile();
		if(nextFile == null && log != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("Found " + matchingFilesCount + " files ("
					+ matchingFilesLength + "b)");
			
			if (scannedDirsCount > 0)
				sb.append(" dirs=" + scannedDirsCount);

			if (scannedFilesCount > 0)
				sb.append(" files=" + scannedFilesCount);
			

			if(rejectedDirsCount > 0)
				sb.append(" rejected dirs=" + rejectedDirsCount);

			if(rejectedFilesCount > 0)
				sb.append(" rejected files=" + rejectedFilesCount);

			log.info(sb.toString());
		}
		return toReturn;
	}

	/**
	 * 
	 * @return A list of paths to directories which contain no files. The paths
	 *         are relative to the scanning parent directory.
	 */
	public List<String> getEmptyDirectories() {
		return emptyDirs;
	}
	

	public String getParentDirectoryPath() {
		return parentDirectoryPath;
	}

	private File findNextMatchingFile() {
		// Similar code also used in Task[&Job]ExecutingHotFolder
		while (true) {
			if (dirFile == null || dirContents == null
					|| dirContentsIndex == dirContents.length) {
				if (dirStack.isEmpty())
					return null;
	
				dirFile = new File(dirStack.remove(dirStack.size() - 1));
				dirContents = dirFile.list();
				dirContentsIndex = 0;
				if (dirContents == null) {
					if (log != null)
						log.warning(dirFile.getAbsolutePath()
								+ " is not a directory, ignoring it");
					continue;
				}
				if (dirContents.length == 0) {
					String absolutePath = dirFile.getAbsolutePath();
					String relativePathname = "";
					if (!parentDirectoryPath.equals(absolutePath)) {
						relativePathname = absolutePath.substring(parentDirectoryPath
								.length() + 1);
					}
					emptyDirs.add(relativePathname);
				}
			}
			
			while (dirContentsIndex < dirContents.length) {
				String filePath = dirContents[dirContentsIndex];
				dirContentsIndex++;

				File file = new File(dirFile, filePath);
				if (file.isDirectory()) {
					scannedDirsCount++;
					if (!recursive) {
						if (log != null)
							log.info("Ignoring " + file.getAbsolutePath());
					} else if(dirFilter != null && (!dirFilter.accept(file))) {
						rejectedDirsCount++;
					} else {
						dirStack.add(file.getAbsolutePath());
					}
				} else if (file.isFile()) {
					++scannedFilesCount;
					if ((log != null && (scannedFilesCount % 10000) == 0))
						log.info("Scanned " + scannedFilesCount
								+ " files, last " + file.getAbsolutePath());

					if (fileFilter != null && (!fileFilter.accept(file))) {
						rejectedFilesCount++;
					} else {
						matchingFilesCount++;
						matchingFilesLength += file.length();
						return file;
					}
				} else {
					if (log != null)
						log.warning("Not normal file \"" + filePath
								+ "\" found in " + dirFile);
				}
			}
		}
	}

}
