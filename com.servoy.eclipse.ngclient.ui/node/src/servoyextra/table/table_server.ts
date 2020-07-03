/**
 * Gets the number of columns
 * 
 * @example
 *	%%prefix%%%%elementName%%.getColumnsCount()
 */ 
public getColumnsCount() {
    return this.columns.length; 
}

/**
 * Gets the column at index. Index is 0 based.
 * 
 * @param index index between 0 and columns length -1
 * 
 * @example
 *	%%prefix%%%%elementName%%.getColumn()
 *	
 * @return {column}
 */ 
public getColumn(index:number) {
	if(this.columns && index >= 0 && index < this.columns.length) {
		return this.columns[index];
	}
	return null;
}

/**
 * Adds new column at specified index. Index is 0 based.
 * 
 * @param dataproviderid dataprovider of the column
 * @param index index between 0 and columns length
 * 
 * @example
 *	var column = %%prefix%%%%elementName%%.newColumn('dataproviderid')
 *
 *	@return {column}
 */
public newColumn(dataproviderid,index:number) {
	 if (!this.columns) this.columns = [];
	 let insertPosition = (index == undefined) ? this.columns.length : ((index == -1 || index > this.columns.length) ? this.columns.length : index);
	 for(let i = this.columns.length; i > insertPosition; i--) {
		  this.columns[i] = this.columns[i - 1]; 
	 }
	 this.columns[insertPosition] = {'dataprovider':dataproviderid};
	 return this.columns[insertPosition];
}

/**
 * Removes column from specified index. Index is 0 based.
 *
 * @example
 * %%prefix%%%%elementName%%.removeColumn(0)
 *
 * @param index index between 0 and columns length -1
 * 
 * @return {boolean}
 */
public removeColumn(index:number) {
	if(index >= 0 && index < this.columns.length) {
		for(let i = index; i <this.columns.length - 1; i++) {
			this.columns[i] = this.columns[i + 1];
		}
		this.columns.length = this.columns.length - 1;
		return true;
	}
	return false;
}

/**
 * Removes all columns.
 *
 * @example
 * %%prefix%%%%elementName%%.removeAllColumns()
 *
 * @return {boolean}
 */
public removeAllColumns() {
	   if(this.columns.length > 0) {
		   this.columns.length = 0;
		   return true;
	   }
	   return false;
}