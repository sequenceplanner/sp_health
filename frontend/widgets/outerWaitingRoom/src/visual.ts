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
import { stackOrderInsideOut } from "d3";
type Selection<T extends d3.BaseType> = d3.Selection<T, any, any, any>;

export class Visual implements IVisual {
    private body: Selection<HTMLElement>;

    private title: Selection<HTMLHeadingElement>;
    private obs: Selection<HTMLHeadingElement>;
    private clock: Selection<HTMLHeadingElement>;
    private svg: any; // the pie chart

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

    private MAX_TEXT_LENGTH_LEGEND = 100;

    constructor(options: VisualConstructorOptions) {
        this.body = d3.select(options.element);
        this.initiateBaseComponents();
        this.initiateClock();
        this.initiatePieChart();
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
        this.row2.append('td').text('Patienter som vårdas:')
        this.patients_treated = this.row2.append('td');

        this.row3 = this.table.append('tr');
        this.row3.append('td').text('Genomsnittliga väntetiden:')
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
     * Connect svg (future pie chart) to the body
     */
    public initiatePieChart() {
        this.svg = this.body.append('svg');
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
     * @param options - this 
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
     * Updates data in table and pie chart
     * @param options - this
     */
    public updateData(options: VisualUpdateOptions) {
        // Get columns with displayNames and row with new data
        let wr_columns = options.dataViews[0].table.columns;
        let wr_rows = options.dataViews[0].table.rows;
        let row = wr_rows[0];

        this.updateTable(wr_columns, row);
        this.updatePieChart(wr_columns, row);
    }

    /**
     * Updates table data to the latest received
     * @param wr_columns - latest data
     * @param row - latest data
     */
    public updateTable(wr_columns, row) {
        // Update widget with new data
        for (let i: number = 0; i < wr_columns.length; i++) {
            switch (wr_columns[i].displayName) {
                case 'nrOfWaiting': {
                    this.nr_patients_waiting = Number(row[i]);
                    this.patients_waiting.text(this.nr_patients_waiting);
                    break;
                }
                case 'nrOfTriaged': {
                    this.nr_patients_treated = Number(row[i]);
                    this.patients_treated.text(this.nr_patients_treated);
                    break;
                }
                case 'averageTTT': {
                    this.current_waiting_time = Number(row[i]);
                    let time: string = this.timeConvert(this.current_waiting_time);
                    this.waiting_time.text(time);
                    break;
                }
            }
        }
    }

    /**
     * Updates data in pie chart to latest received data, only viewing hospitals wards with over zero patients.
     * 
     * The method creates a data instance that collects all the different hospital wards recevied in wr_columns
     * together with their amount of people treated from row. Then sends this instance to updatePieChartData-method
     * to get updated in the svg.
     *
     * @param wr_columns latest data
     * @param row latest data
     */
    public updatePieChart(wr_columns, row) {
        var hasPieChart = false;
        var data: any = {};
        var displayNames = wr_columns.map(col => col.displayName);

        let medIndex = displayNames.indexOf('medicin');
        if (medIndex != -1 && row[medIndex]) {
            data['Medicin'] = row[displayNames.indexOf('medicin')];
            hasPieChart = true;
        }
        let kirIndex = displayNames.indexOf('kirurgi');
        if (kirIndex != -1 && row[kirIndex]) {
            data['Kirurgi'] = row[displayNames.indexOf('kirurgi')];
            hasPieChart = true;
        }
        let ortIndex = displayNames.indexOf('ortopedi');
        if (ortIndex != -1 && row[ortIndex]) {
            data['Ortopedi'] = row[displayNames.indexOf('ortopedi')];
            hasPieChart = true;
        }
        let jourIndex = displayNames.indexOf('jour');
        if (jourIndex != -1 && row[jourIndex]) {
            data['Barn/Gyn/ÖNH'] = row[displayNames.indexOf('jour')];
            hasPieChart = true;
        }
        let annIndex = displayNames.indexOf('annat');
        if (annIndex != -1 && row[annIndex]) {
            data['Övrigt'] = row[displayNames.indexOf('annat')];
            hasPieChart = true;
        }

        // Update pieChart with the updated data
        this.updatePieChartData(data);

        // Only present pie chart if ward data has been sent
        if (hasPieChart) {
            this.svg.append("text")
                .attr("x", 0)
                .attr("y", -120)
                .attr("text-anchor", "middle")
                .style("fill", '#979797')
                .style("font-size", this.scaleFactor / 28 + "px")
                .text("Antal patienter på varje avdelning:");
        }
    }

    /**
     * Version 2 of updating the pie chart.
     * 
     * Can be used if the dataset has an array instance of all departments, 
     * rather than deciding beforehand what departments should be presented and have
     * them as stand-alone instances.
     * 
     * This solution is currently not in use since MPBi can't handle nestled lists.
     */
    public updatePieChartViaArray(wr_columns, wr_rows) {
        // Getting the indexes of department and nrOfPatients
        var displayNames = wr_columns.map(col => col.displayName);
        var departmentIndex = displayNames.indexOf('department');
        var nrOfPatientsIndex = displayNames.indexOf('nrOfPatients');
        console.log(displayNames);
        console.log(departmentIndex);
        console.log(nrOfPatientsIndex);

        // Creating dataset of departments and nrOfPatients
        var data: any = {};
        for (let j: number = 0; j < wr_rows.length; j++) {
            let r = wr_rows[j];
            if (r[nrOfPatientsIndex] != 0) {
                data[<any>(r[departmentIndex])] = r[nrOfPatientsIndex];
            }
        }
        console.log(data);

        // Update pieChart with the updated data
        this.updatePieChartData(data);
    }

    /**
     * Updates the clock to current time
     */
    public updateTime() {
        this.clock.text(this.getTime());
    }

    /**
     * Creates a pie chart with the current data and appends it to the pie chart svg.
     * 
     * @param data hospital wards and the amount of patients in each ward
     */
    public updatePieChartData(data) {
        // set the dimensions and margins of the graph
        var width = 0.578 * this.scaleFactor;
        var height = 0.419 * this.scaleFactor;
        var margin = 40;

        let self = this;

        // The radius of the pie chart is half the width or half the height (smallest one). I subtract a bit of margin.
        var radius = (Math.min(width, height) / 2 - margin);

        // setting width and height
        d3.select("svg").remove();
        this.svg = this.body.append('svg')
            .attr("width", width)
            .attr("height", height)
            .append("g")
            .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

        // set the color scale
        var color = d3.scaleOrdinal()
            .domain(Object.keys(data))
            .range(["#4b94de", "#de6464", "#5ed198", "#ec98ed", "#d6c358"])

        // Compute the position of each group on the pie:
        var pie: any
        pie = d3.pie().value(function (d) {
            return (<any>d).value;
        });
        var data_ready = pie(d3.entries(data));

        // shape helper to build arcs:
        var arcGenerator: any;
        arcGenerator = d3.arc()
            .innerRadius(5)
            .outerRadius(radius)
            .padAngle(0.02)
            .padRadius(radius)
            .cornerRadius(4);

        // Build the pie chart: Basically, each part of the pie is a path that we build using the arc function.
        this.svg
            .selectAll('mySlices')
            .data(<any>data_ready)
            .enter()
            .append('path')
            .attr('d', arcGenerator)
            .attr('fill', <any>function (d) {
                return (color(d.data.key))
            })
            .attr("transform", function (d, i) {
                return "translate(" + (-0.09 * self.scaleFactor) + "," + 0 + ")";
            });

        // Now add the annotation. Use the centroid method to get the best coordinates
        this.svg
            .selectAll('mySlices')
            .data(data_ready)
            .enter()
            .append('text')
            .text(function (d) {
                return (<any>d).data.value
            })
            .attr("transform", function (d) {
                return "translate(" + (arcGenerator.centroid(d)[0] + (-0.09 * self.scaleFactor)) + "," + (arcGenerator.centroid(d)[1] + (0.005 * self.scaleFactor)) + ")";
            })
            .style("text-anchor", "middle")
            .style("font-size", 24)
            .style("font-weight", "normal")
            .style("fill", "white");

        var legendG = this.svg.selectAll('.legend')
            .data(data_ready)
            .enter().append("g")
            .attr("transform", function (d, i) {
                return "translate(" + (0.1125 * self.scaleFactor) + "," + ((i - 2) * 32) + ")";
            })
            .attr("class", "legend");

        legendG.append("circle")
            .attr("r", 8)
            .attr("cx", 0)
            .attr("cy", 8)
            .attr("fill", function (d) {
                return color(d.data.key);
            });

        legendG.append("text")
            .text(function (d) {
                return (<any>d).data.key;
            })
            .style("font-size", 20)
            .attr("x", 16)
            .attr("y", 16)
            .style("fill", "#979797")
            .each(function () {
                let textLength = d3.select(this).node().getComputedTextLength();
                let text = d3.select(this).text();

                while (textLength > self.MAX_TEXT_LENGTH_LEGEND && text.length > 0) {
                    text = text.slice(0, -1);
                    d3.select(this).text(text + '.');
                    textLength = d3.select(this).node().getComputedTextLength();
                }
            });
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