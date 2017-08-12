package ch.elexis.core.ui.importer.div.importers.multifile.strategy;

import java.io.File;
import java.util.Map;

import ch.elexis.core.ui.importer.div.importers.multifile.IMultiFileParser;
import ch.rgw.tools.Result;

/**
 * Import Strategy for a file
 * 
 * @author lucia
 * 		
 */
public interface IFileImportStrategy {
	
	/**
	 * executes the import for this file
	 * 
	 * @param file
	 *            {@link File}
	 * @param context
	 *            containing any needed information to perform the import. See
	 *            {@link IMultiFileParser} constants.
	 * @return {@link Result} indicating whether import succeeded or not
	 */
	public Result<Object> execute(File file, Map<String, Object> context);
	
	/**
	 * Specify if imported files should be moved to archive and error directory inside the import
	 * directory. Default is false.
	 * 
	 * @param value
	 * @return
	 */
	public IFileImportStrategy setMoveAfterImport(boolean value);
	
	public void setTestMode(boolean testing);
	
}
