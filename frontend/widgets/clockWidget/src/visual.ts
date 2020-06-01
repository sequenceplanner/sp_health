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
import "./../style/visual.less";
import powerbi from "powerbi-visuals-api";
import VisualConstructorOptions = powerbi.extensibility.visual.VisualConstructorOptions;
import VisualUpdateOptions = powerbi.extensibility.visual.VisualUpdateOptions;
import IVisual = powerbi.extensibility.visual.IVisual;

import * as d3 from "d3";
type Selection<T extends d3.BaseType> = d3.Selection<T, any, any, any>;

export class Visual implements IVisual {
    private body: Selection<HTMLElement>;
    private clock: Selection<HTMLParagraphElement>;

    constructor(options: VisualConstructorOptions) {
        this.body = d3.select(options.element);

        // Append clock to the body and present current time
        this.clock = this.body.append('p').text(this.getTime());

        //Setting how often the clock should update it's time
        setInterval(() => { this.updateTime() }, 1000);
    }

    public updateTime() {
        this.clock.text(this.getTime());
    }

    /**
     * Updates the size of the clock according to window
     * @param options 
     */
    public update(options: VisualUpdateOptions) {
        // Sets width and height to widget-size
        let width: number = options.viewport.width;
        let height: number = options.viewport.height;
        this.body.attr("width", width);
        this.body.attr("height", height);
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