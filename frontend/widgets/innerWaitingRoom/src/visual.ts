/*
*  Power BI Visual CLI
*
*  Copyright (c) Microsoft Corporation
*  All rights reserved.
*  MIT License
*
*  Permission is hereby granted, free of charge, to any person obtaining a copy
*  of this software and associated documentation files (the ""Software""), to deal
*  in the Software without restriction, including without limitation the rights
*  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*  copies of the Software, and to permit persons to whom the Software is
*  furnished to do so, subject to the following conditions:
*
*  The above copyright notice and this permission notice shall be included in
*  all copies or substantial portions of the Software.
*
*  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
*  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
*  THE SOFTWARE.
*/
"use strict";
import "core-js/stable";
import "../style/visual.less";
import powerbi from "powerbi-visuals-api";
import IVisual = powerbi.extensibility.IVisual;
import VisualConstructorOptions = powerbi.extensibility.visual.VisualConstructorOptions;
import VisualUpdateOptions = powerbi.extensibility.visual.VisualUpdateOptions;

import * as d3 from "d3";
type Selection<T extends d3.BaseType> = d3.Selection<T, any, any, any>;

export class Visual implements IVisual {
    private body: Selection<HTMLElement>;

    private title: Selection<HTMLHeadingElement>;
    private obs: Selection<HTMLHeadingElement>;
    private clock: Selection<HTMLHeadingElement>;

    private table: Selection<HTMLTableElement>;
    private row1: Selection<HTMLTableRowElement>; // row for patients in waiting room
    private row2: Selection<HTMLTableRowElement>; // row for patients being treated
    private row3: Selection<HTMLTableRowElement>; // row for the current waiting time

    private patients_waiting: Selection<HTMLTableCellElement> // cell for the nr of patients waiting
    private patients_treated: Selection<HTMLTableCellElement> // cell for the nr of patients treated
    private waiting_time: Selection<HTMLTableCellElement> // cell for the waiting time

    private nr_patients_waiting: number;
    private nr_patients_treated: number;
    private current_waiting_time: number;

    private scaleFactor: number; // scale used for resizing elements in the visual

    constructor(options: VisualConstructorOptions) {
        this.body = d3.select(options.element);

        this.initiateBaseComponents();
        this.initiateClock();
    }

    /**
     * Initiates all text components in the visual which includes title, table info and OBS text,
     * and appends it to the body.
     */
    public initiateBaseComponents() {
        this.title = this.body.append('h1').text('Läget på akutmottagningen');

        this.table = this.body.append('table');

        this.row1 = this.table.append('tr');
        this.row1.append('td').text('Patienter i väntrummet:')
        this.patients_waiting = this.row1.append('td');

        this.row2 = this.table.append('tr');
        this.row2.append('td').text('Patienter som vårdas på avdelningen:')
        this.patients_treated = this.row2.append('td');

        this.row3 = this.table.append('tr');
        this.row3.append('td').text('Genomsnittliga väntetiden till läkarmöte:')
        this.waiting_time = this.row3.append('td');

        this.obs = this.body.append('h2')
            .text('OBS! Alla tider är uppskattningar och kan avvika från verkligheten.');
    }

    /**
     * Append the clock to the body and starts it.
     */
    public initiateClock() {
        this.clock = this.body.append('h3').text(this.getTime());
        setInterval(() => { this.updateTime() }, 1000);
    }

    /**
     * Update function for the visual, both changing size of the elements and their current values.
     * @param options this
     */
    public update(options: VisualUpdateOptions) {
        this.updateSizeOfComponents(options);
        this.updateData(options);
    }

    /**
     * Gets width and height of the current visual and updates alla components to match.
     * @param options this
     */
    public updateSizeOfComponents(options: VisualUpdateOptions) {

        // Sets width and height to widget-size
        let width: number = options.viewport.width;
        let height: number = options.viewport.height;
        this.body.attr("width", width);
        this.body.attr("height", height);

        this.scaleFactor = Math.min(width, height);

        // Updates components to window size
        let titleSize: number = this.scaleFactor / 13;
        this.title.style("font-size", titleSize + "px");

        let tableSize: number = this.scaleFactor / 19;
        this.table.style("font-size", tableSize + "px");

        let obsSize: number = this.scaleFactor / 28;
        this.obs.style("font-size", obsSize + "px");

        let clock: number = this.scaleFactor / 10;
        this.clock.style("font-size", clock + "px");
    }

    /**
     * Gets the latest data and updates the table
     * @param options this
     */
    public updateData(options: VisualUpdateOptions) {
        // Get columns with displayNames and row with new data
        let wr_columns = options.dataViews[0].table.columns;
        let wr_rows = options.dataViews[0].table.rows;
        let row = wr_rows[0];

        this.updateVisualData(wr_columns, row);
    }

    /**
     * Update table data and the title to match the actual hospital department
     * and their latest data 
     * @param wr_columns latest data
     * @param row latest data
     */
    public updateVisualData(wr_columns, row) {
        // Update widget with new data
        for (let i: number = 0; i < wr_columns.length; i++) {
            switch (wr_columns[i].displayName) {
                case 'waiting': {
                    this.nr_patients_waiting = Number(row[i]);
                    this.patients_waiting.text(this.nr_patients_waiting);
                    break;
                }
                case 'treated': {
                    this.nr_patients_treated = Number(row[i]);
                    this.patients_treated.text(this.nr_patients_treated);
                    break;
                }
                case 'averageTTL': {
                    this.current_waiting_time = Number(row[i]);
                    let time: string = this.timeConvert(this.current_waiting_time);
                    this.waiting_time.text(time);
                    break;
                }
                case 'department': {
                    // The different hard coded cases can be left out, and coloring can be implemented directly in mpbi
                    if (row[i] == 'Medicin2') {
                        this.title.text("INRE VÄNTRUM - MEDICIN");
                        this.body.style("background-color", "#4b94de");
                        break;
                    }
                    if (row[i] == 'Kirurgi') {
                        this.title.text("INRE VÄNTRUM - " + row[i]);
                        this.body.style("background-color", "#de6464");
                        break;
                    }
                    if (row[i] == 'Medicin3') {
                        this.title.text("INRE VÄNTRUM - MEDICIN");
                        this.body.style("background-color", "#d6c358");
                        break;
                    }
                    if (row[i] == 'Ortopedi') {
                        this.title.text("INRE VÄNTRUM - " + row[i]);
                        this.body.style("background-color", "#5ed198");
                        break;
                    }

                    if (row[i] == 'Jour/Medicin4') {
                        this.title.text("INRE VÄNTRUM - JOUR/MEDICIN");
                        this.body.style("background-color", "#ec98ed");
                        break;
                    }
                    this.title.text('INRE VÄNTRUM');
                    break;
                }
            }
        }
    }

    /**
     * Sets clock object to current time
     */
    public updateTime() {
        this.clock.text(this.getTime());
    }

    /**
     * Converts seconds to hours and minutes.
     * @param time in seconds
     */
    public timeConvert(time: number) {
        let hours = (time / 60);
        let h = Math.floor(hours);
        let minutes = (hours - h) * 60;
        let m = Math.round(minutes);
        if (h == 0) {
            return time + 'min';
        }
        return h + 'h ' + m + 'min';
    }

    /**
     * Returns current clock time
     */
    public getTime() {
        let date = new Date();
        let hours = date.getHours().toString();
        let minutes = date.getMinutes().toString();

        if (hours.length < 2) {
            hours = '0' + hours;
        }
        if (minutes.length < 2) {
            minutes = '0' + minutes;
        }
        return hours + ":" + minutes;
    }
}