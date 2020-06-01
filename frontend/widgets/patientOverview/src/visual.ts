"use strict";
import "core-js/stable";
import "../style/visual.less";
import powerbi from "powerbi-visuals-api";
import IVisual = powerbi.extensibility.IVisual;
import DataViewMetadataColumn = powerbi.DataViewMetadataColumn;
import DataViewTableRow = powerbi.DataViewTableRow;
import DataViewTable = powerbi.DataViewTable;
import VisualConstructorOptions = powerbi.extensibility.visual.VisualConstructorOptions;
import VisualUpdateOptions = powerbi.extensibility.visual.VisualUpdateOptions;

import { VisualSettings } from "./settings";
import VisualObjectInstanceEnumeration = powerbi.VisualObjectInstanceEnumeration;
import EnumerateVisualObjectInstancesOptions = powerbi.EnumerateVisualObjectInstancesOptions;

import * as d3 from "d3";
import { rgb, ScaleLinear, Numeric, sum, BaseType } from "d3";

type Selection<T extends d3.BaseType> = d3.Selection<T, any,any, any>;

/* Basic datastructure to hold a row in the table. The lenght of the array values should
correspond to the number of columns in the table. The label describes the title of the row e.g.
{label: nr_of_patients, values: 0, 7, 4, 11} */
interface TableRow {
    label: string; 
    values: any[]; 
}

export class Visual implements IVisual {
    private body: Selection<HTMLElement>; 
    private errorMessage: Selection<HTMLHeadingElement>; 
    private table: Selection<HTMLTableElement>; 
    private thead: Selection<HTMLTableRowElement>; 
    private tbody: Selection<HTMLTableSectionElement>; 
    private emptyCards: Selection<BaseType>; 
    private nonDepartments: Selection<BaseType>; 

    private visualSettings: VisualSettings;

    private nrOfCols: number; 

    private viewWidth: number; 
    private viewHeight: number; 

    private patients_data: DataViewTable; 
    private triageLabels: string[] = ['red_patients', 'orange_patients', 'yellow_patients', 'green_patients', 'blue_patients', 'grey_patients']; 
    private headerLabels: string[] = ['nr_of_patients', 'unattended', 'attended', 'finished']; 
    private deptNames: string[];  

    constructor(options: VisualConstructorOptions) {
        // Body 
        this.body = d3.select(options.element); 

        // Error message placeholder
        this.errorMessage = this.body.append('h3')
            .attr('class', 'error');  

        // Empty cards on top
        this.emptyCards = this.body.append('container').attr('class', 'emptyCards'); 

        // Main table 
        this.table = this.body.append('table');
        this.thead = this.table.append('thead').append('tr');
        this.tbody = this.table.append('tbody');

        // Non departments on bottom 
        this.nonDepartments = this.body.append('container').attr('class', 'nonDepartments')
    }
    
    public enumerateObjectInstances(options: EnumerateVisualObjectInstancesOptions): VisualObjectInstanceEnumeration {
        const settings: VisualSettings = this.visualSettings || <VisualSettings>VisualSettings.getDefault();
        return VisualSettings.enumerateObjectInstances(settings, options);
    }

        /* Update is automatically called whenever the dataset changes */
    public update(options: VisualUpdateOptions) {

        // Update patients data 
        this.patients_data = options.dataViews[0].table; 

        // Adjust the visual settings
        this.adjustVisualSettings(options); 

        // Update height and width of visual component 
        this.viewWidth = options.viewport.width;
        this.viewHeight = options.viewport.height;

        // Update deparment names with or without empty departments
        this.visualSettings.patientView.minimizeEmptyDepts ? this.updateDepartmentNames(false) : this.updateDepartmentNames(true); 

        // Make data validation 
        let dataIsValid: boolean = this.dataIsValid(); 

        // If no errors occurred, show visual  
        if (dataIsValid) {
            this.renderVisuals(); 
        } else {
            console.log('Data is invalid'); 
        }
    }

    /* Renders all the visuals */ 
    private renderVisuals() {
        this.errorMessage.text(""); // Remove any error message
        if (this.visualSettings.patientView.minimizeEmptyDepts) {
            this.addEmptyCards(); // Add empty cards 
        } else {
            this.emptyCards.html(""); //Remove empty cards
        }
        this.addOtherFields(); // Add other visuals, e.g. stream/trauma
        this.addHeaders(); // Add table headers
        this.addRows(); // Add table rows
    }

    /* Adjusts given values for fontsizes to the scale 0-40. 
    E.g. if user enters a value larger than 40 it is automatically adjusted to 40. */ 
    private adjustVisualSettings(options: VisualUpdateOptions) {
        this.visualSettings = VisualSettings.parse<VisualSettings>(options.dataViews[0]);   
        this.visualSettings.patientView.fontSizeTitle = Math.max(0, this.visualSettings.patientView.fontSizeTitle);
        this.visualSettings.patientView.fontSizeTitle = Math.min(40, this.visualSettings.patientView.fontSizeTitle);
        
        this.visualSettings.patientView.fontSizeFigures = Math.max(0, this.visualSettings.patientView.fontSizeFigures);
        this.visualSettings.patientView.fontSizeFigures = Math.min(40, this.visualSettings.patientView.fontSizeFigures);
        
        this.visualSettings.patientView.fontSizeTotalCount = Math.max(0, this.visualSettings.patientView.fontSizeTotalCount);
        this.visualSettings.patientView.fontSizeTotalCount = Math.min(40, this.visualSettings.patientView.fontSizeTotalCount);
    }

    /* Add smaller cards for empty departments at top of visual */ 
    private addEmptyCards() {
        let emptyDepartments: string[] = this.emptyDepartments();

        this.emptyCards.selectAll('div')
            .data(emptyDepartments)
            .join(
                enter => enter.append('div')
                    .style('font-size', this.visualSettings.patientView.fontSizeTitle + 'px')
                    .attr('class', 'emptyCard')
                    .style("opacity", '0')
                    .style("position", "relative")
                    .style("top", '-30px')
                    .text(d => d)
                    .call(enter => enter.transition()
                        .duration(500)
                        .style("top", '0px')
                        .style("opacity", '1')),
                update => update
                    .text(d => d)
                    .style('font-size', this.visualSettings.patientView.fontSizeTitle + 'px')
                    .attr('class', 'emptyCard'),
                exit => exit
                    .transition()
                    .duration(500)
                    .style('opacity', '0')
                    .style("top", '-30px')
                    .remove()
            ); 
    }

    /* Add boxes for variables such as Stream/Trauma */ 
    private addOtherFields() {
        let otherFields: string[] = this.otherFields();

        this.nonDepartments.selectAll('div')
            .data(otherFields)
            .join(
                enter => enter.append('div')
                    .style('font-size', this.visualSettings.patientView.fontSizeTitle + 'px')
                    .attr('class', 'otherFieldCard')
                    .style("opacity", '0')
                    .style("position", "relative")
                    .style("top", '-30px')
                    .text(d => d + ': ' + this.valueOfField(d, 'nr_of_patients'))
                    .call(enter => enter.transition()
                        .duration(500)
                        .style("top", '0px')
                        .style("opacity", '1')),
                update => update
                    .text(d => d + ': ' + this.valueOfField(d, 'nr_of_patients'))
                    .style('font-size', this.visualSettings.patientView.fontSizeTitle + 'px')
                    .attr('class', 'otherFieldCard'),
                exit => exit
                    .transition()
                    .duration(500)
                    .style('opacity', '0')
                    .style("top", '-30px')
                    .remove()
            ); 
    }

    /* Draws the header of the table */ 
    private addHeaders() {
        let headerArray: TableRow[] = this.headerArray();

        var headerCols = this.thead.selectAll('td')
        .data(headerArray)
        .join(
            enter => enter.append('td')
                .attr('style', 'font-size: ' + this.visualSettings.patientView.fontSizeTitle + 'px')
                .attr('class', d => d.label == 'totalt' ? 'thead sum' : 'thead')
                // .attr('class', 'thead sum')
                .style("opacity", '0')
                .text(d => d.label)
                .call(enter => enter.transition()
                    .duration(1000)
                    .style("opacity", '1')),
            update => update 
                .attr('style', 'font-size: ' + this.visualSettings.patientView.fontSizeTitle + 'px')
                .attr('class', d => d.label == 'totalt' ? 'thead sum' : 'thead')
                //.attr('class', 'thead sum')
                .text(d => d.label),
            exit => exit.remove()
        ); 

        headerCols.selectAll('tr')
            .data(function (row) { return row.values })
            .join(
                enter => enter.append('tr')
                    .text(d => d[0])
                    .attr('frequency', d => d[1] == 0 ? '' : d[1])
                    .attr('style', (d,i) => {
                        return i == 0 ? 'font-size: ' + this.visualSettings.patientView.fontSizeTotalCount + 'px' : ""; 
                    })
                    .attr('class', (d,i) => {
                        switch(i) {
                            case 0: return 'title'; 
                            case 1: return 'unattended'; 
                            case 2: return 'attended'; 
                            case 3: return 'finished'; 
                            default: return ''; 
                        }
                    }),
                update => update
                    .attr('style', (d,i) => {
                        return i == 0 ? 'font-size: ' + this.visualSettings.patientView.fontSizeTotalCount + 'px' : ""; 
                    })         
                    .attr('frequency', d => d[1] == 0 ? '' : d[1])
                    .attr('class', (d,i) => {
                        switch(i) {
                            case 0: return 'title'; 
                            case 1: return 'unattended'; 
                            case 2: return 'attended'; 
                            case 3: return 'finished'; 
                            default: return ''; 
                        }
                    })
                    .style("position", "relative")
                    .style("top", '-30px')
                    .text(d => d[0])
                    .call(update => update.transition()
                        .delay(500)
                        .style("top", '0px')),
                exit => exit.remove() 
            ); 
    }

    /* Draws the colored rows of the table which show triage levels */ 
    private addRows() {
        let triageArray: TableRow[] = this.triageArray(); 
        let nrOfColumns: number = triageArray[0].values.length; 

        var rows = this.tbody.selectAll('tr')
            .data(triageArray)
            .join(
                enter => enter.append('tr')
                    // split the data label, for example 'red_patients' to red
                    .attr('class', (d) => { return d.label.split('_')[0] } ),
                update => update
                    .attr('class', (d) =>  { return d.label.split('_')[0] }),
                exit => exit.remove()
            ); 

        rows.selectAll('td')
            .data(function (row) { return row.values })
            .join(
                enter => enter.append('td')
                    .attr("width", (this.viewWidth) / (this.nrOfCols + 1))
                    .attr('style', d => { return d == 0 ? 'color: grey' : '' })
                    .attr('style', 'font-size: ' + this.visualSettings.patientView.fontSizeFigures + 'px')
                    // Color first column differently 
                    .attr('class', (d,i) =>  { return i == 0 ? 'sum' : '' })
                    .style("opacity", '0')
                    .text(d => d)
                    .call(enter => enter.transition()
                        .duration(1000)
                        .style("opacity", '1')),
                update => update
                    .attr("width", (this.viewWidth) / (this.nrOfCols + 1))
                    .attr('style', d => d == 0 ? 'color: grey' : '' )
                    .attr('style', 'font-size: ' + this.visualSettings.patientView.fontSizeFigures + 'px')
                    // Color first column differently 
                    .attr('class', (d,i) =>  { return i == 0 ? 'sum' : '' })
                    .text(d => d),
                exit => exit.remove(),
            )

    }

    /* Method that fetches all department names from the data view 
       Each row corresponds to one department and the name of the department 
       is stored as one value in the row array. The index of the deparment
       name can be found in the columns array. 

       Parameter includeEmpty decides if empty departments are shown in the 
       table or as minimized cards on top of table, in which case they are
       not included in the list of department names.
    */ 
    private updateDepartmentNames(includeEmpty: boolean) {
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        let departments: any[] = new Array(); 
        let index: number = 1; 
        rows
            .filter(row => row[this.indexOfColumn('is_department')] === 'true'
            && (includeEmpty || row[this.indexOfColumn('nr_of_patients')] != 0))
            .map(row => row[this.indexOfColumn('department')]) // map row to department name
            .sort() // sort alphabetically
            .forEach(dept => departments[index++] = dept); 

        departments[0] = 'totalt';
        this.nrOfCols = index;  
        
        this.deptNames = departments; 
        console.log('Department names are :' + this.deptNames); 
    }

    /* Returns an array of every department thas currently has no patients */ 
    private emptyDepartments(): string[] {
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        let emptyDept: string[] = new Array(); 
        let index: number = 0; 
        rows
            .filter(row => row[this.indexOfColumn('is_department')] === 'true' && row[this.indexOfColumn('nr_of_patients')] == 0) // Find empty departments
            .map(row => row[this.indexOfColumn('department')]) // Map row to department name
            .sort() // Sort alphabetically
            .forEach(dept => emptyDept[index++] = dept.toString()); 
        console.log('empty dept: '); 
        console.log(emptyDept); 
        return emptyDept; 
    }


    /* Returns an array of every department thas currently has no patients */ 
    private otherFields(): string[] {
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        let otherFields: string[] = new Array(); 
        let index: number = 0; 
        rows
            .filter(row => row[this.indexOfColumn('is_department')] === 'false') // Find columns which are not viewed as departments
            .map(row => row[this.indexOfColumn('department')]) // Map row to name of field
            .sort() // Sort alphabetically
            .forEach(dept => otherFields[index++] = dept.toString()); 
        return otherFields; 
    }


    /* Returns the index of the column where data can be found associated with the given data type  */ 
    private indexOfColumn(columnName: string): number {
        const columns: DataViewMetadataColumn[] = this.patients_data.columns; 

        for (let i: number = 0; i < columns.length; i ++) {
            let column: DataViewMetadataColumn = columns[i]; 
            if (column.displayName === columnName) {
                return i; 
            }
        }
        return null; 
    }

    /* Returns the index of the row where data can be found associated with the given department  */ 
    private indexOfRow(department: string) {
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        let deptIndex = this.indexOfColumn('department');
        for (let i: number = 0; i < rows.length; i ++) {
            let row: DataViewTableRow = rows[i]; 
            if (row[deptIndex] === department) {
                return i; 
            }
        }
        return null; 
    }

    /* Returns true if the given columnName is among the provided columns, else false */
    private columnsContain(columnName: string): boolean {
        let columns: DataViewMetadataColumn[] = this.patients_data.columns; 
        return columns
            .map(c => c.displayName)
            .find(s => s === columnName) === columnName; 
    }

    /* Returns true if the given columnName is among the provided columns, else false */
    private departmentsContain(deptName: string): boolean { 
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        return (rows
            .map(row => row[this.indexOfColumn('department')]) // Map row to department name
            .findIndex(s => s === deptName)) > 0; 
    }

    /* Returns the value of a given field defined by its column and department name, 
    e.g. a call with department: medicin and column: nr_of_patients returns the number of patients for medicin*/ 
    private valueOfField(department: string, column: string ): number {
        if (department === 'totalt') {
            return this.sumOfColumn(column); 
        }
        const rows: DataViewTableRow[] = this.patients_data.rows; 

        let rowIndex = this.indexOfRow(department); 
        if (rowIndex == null) {
            console.log(department + ' could not be found in dataset.')
            return null; 
        }
        let row: any[] = rows[rowIndex]; 
        let colIndex = this.indexOfColumn(column); 
        if (colIndex == null) {
            console.log(column + ' could not be found in dataset.')
            return null; 
        }
        return row[colIndex]; 
    }

    /* Returns the total value of a column, e.g. nr_of_patients, for all departments */ 
    private sumOfColumn(column: string): number {
        const rows: DataViewTableRow[] = this.patients_data.rows; 
        let colIndex: number = this.indexOfColumn(column); 
        let deptIndex: number = this.indexOfColumn('department'); 
        let sum: number = 0; 
        rows
            .filter(row => row[deptIndex] != 'Stream' && row[deptIndex] != 'Trauma')
            .forEach(row => sum += <number> row[colIndex]); 
        return sum; 
    }

    /* Returns an array holding all the data necessary to draw the colored triage rows */ 
    private triageArray() : TableRow[] {
        let triageArray: TableRow[]; 
        if (this.visualSettings.patientView.hideBluePatients) {  // Blue patients are shown as green 
            triageArray = new Array(this.triageLabels.length - 1);  
            let arrayIndex = 0; 
            for (let rowIndex: number = 0; rowIndex < this.triageLabels.length; rowIndex++) { 
                let triageLabel = this.triageLabels[rowIndex]; 
                if (triageLabel != 'blue_patients') { 
                    let row: number[] = new Array(this.deptNames.length); 
                    for (let colIndex: number = 0; colIndex < this.deptNames.length; colIndex++) { // column index 
                        let deptName = this.deptNames[colIndex]; 
                        row[colIndex] = (triageLabel == 'green_patients') ? 
                            this.valueOfField(deptName, 'green_patients') + this.valueOfField(deptName, 'blue_patients') : 
                            this.valueOfField(deptName, triageLabel); 
                    }
                    triageArray[arrayIndex] = {label: triageLabel, values: row};
                    arrayIndex++;  
                } 
            }
        } else { // Blue patients are shown seperately 
            triageArray = new Array(this.triageLabels.length);  
            for (let i: number = 0; i < this.triageLabels.length; i++) { // row index 
                let row: number[] = new Array(this.deptNames.length); 
                for (let j: number = 0; j < this.deptNames.length; j++) { // column index  
                    row[j] = this.valueOfField(this.deptNames[j], this.triageLabels[i]); 
                }
                triageArray[i] = {label: this.triageLabels[i], values: row}; 
            }
        }
        return triageArray; 
    }

    /* Returns an array holding all the data necessary to draw the headers */ 
    private headerArray() : TableRow[] {
        let headerArray: TableRow[] = new Array(this.deptNames.length);  
            for (let i: number = 0; i < this.deptNames.length; i++) { // row index 
                let row: any[] = new Array(this.headerLabels.length); 
                for (let j: number = 0; j < this.headerLabels.length; j++) { // column index  
                    if (this.headerLabels[j] == 'nr_of_patients') {
                        // Formats row as tuple, value 0 represents non-existing frequency 
                        row[j] = [this.valueOfField(this.deptNames[i], this.headerLabels[j]), 0];
                    } else {
                        row[j] = this.formatFrequencies(
                            this.valueOfField(this.deptNames[i], 
                            this.headerLabels[j]), 
                            this.visualSettings.patientView.showFrequencies ? this.valueOfField(this.deptNames[i], this.headerLabels[j]+'_change') : 0);
                    } 
                }
                headerArray[i] = {label: this.deptNames[i], values: row}; 
            }
        return headerArray; 
    }

    private formatFrequencies(value: number, change: number) :[string, string] {
        if (change > 0) {
            return [value.toString(), '+' + change];
        } else if (change < 0) {
            return [value.toString(), change.toString()];
        } else {
            return [value.toString(), '0'];
        }
    }

    /* Performs a number of checks on the new data view to determine if it fits the criterias */ 
    private dataIsValid(): boolean {

        let dataIsValid: boolean = true;  

        if (this.patients_data.rows.length == 0) {
            this.errorMessage.text('Inga rader hittades.');  
            dataIsValid = false; 
        }        
        
        if (this.patients_data.columns.length == 0) {
            this.errorMessage.text('Inga kolumner hittades.');  
            dataIsValid = false; 
        }

        /* Check that all required columns are present in the new dataset */
        if (!this.columnsContain('nr_of_patients') 
        || !this.columnsContain('unattended')
        || !this.columnsContain('attended') 
        || !this.columnsContain('finished') 
        || !this.columnsContain('red_patients') 
        || !this.columnsContain('orange_patients')  
        || !this.columnsContain('yellow_patients') 
        || !this.columnsContain('green_patients') 
        || !this.columnsContain('grey_patients')
        || !this.columnsContain('blue_patients')
        || !this.columnsContain('is_department')) {
            this.errorMessage
                .text('Indatan matchar inte datamodellen. Kontrollera att följande datavärden är associerade med vyn: \n' +
                'nr_of_patients, unattended, attended, finished, red_patients, orange_patients, yellow_patients, green_patients, blue_patients, grey_patients, is_department');
            dataIsValid = false; 
        }

        /* If no 'department' column is present, there is no way to group the values */
        if (!this.columnsContain('department')) {
            this.errorMessage.text('Department måste väljas som kolumnvärde');
            dataIsValid = false; 
        }
        return dataIsValid; 
    }

}