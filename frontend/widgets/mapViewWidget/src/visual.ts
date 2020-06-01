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

// Import various libraries and packages
import "core-js/stable";
import "./../style/visual.less";
import * as _fs from "fs";
import * as d3 from "d3";
import * as d3plus from "d3plus-text";
import powerbi from "powerbi-visuals-api";
import VisualConstructorOptions = powerbi.extensibility.visual.VisualConstructorOptions;
import VisualUpdateOptions = powerbi.extensibility.visual.VisualUpdateOptions;
import IVisual = powerbi.extensibility.visual.IVisual;
import EnumerateVisualObjectInstancesOptions = powerbi.EnumerateVisualObjectInstancesOptions;
import VisualObjectInstance = powerbi.VisualObjectInstance;
import DataView = powerbi.DataView;
import VisualObjectInstanceEnumerationObject = powerbi.VisualObjectInstanceEnumerationObject;

// Import types from other files in the project
import { VisualSettings } from "./settings";
import { RoomMap, Room, Attributes } from "./types";

export class Visual implements IVisual {

    public scaleFactor: number;
    public offsetFactor: number;

    // Define constants for drawing the map view nicely
    public paddingRatio = 0.06;
    public textRatio = 0.7;
    public titleTextRatio = 0.8;
    public occupiedRoomOpacity = 1;
    public vacantRoomOpacity = 0.4;
    public occupiedRoomTextColor = "white";
    public vacantRoomTextColor = "black";

    private settings: VisualSettings;

    // Instantiate the object that contains the array of rooms in the hospital
    private rooms: RoomMap = {
        rooms: []
    };

    // This method should get called when the widget is created.  Stuff should be set up/initialized here
    constructor(options: VisualConstructorOptions) {
        console.log('Visual constructor', options);
    }

    // This method gets called when the wdiget gets new data or is refreshed
    public update(options: VisualUpdateOptions) {
        this.settings = Visual.parseSettings(options && options.dataViews && options.dataViews[0]);

        // Parse the data that we receive and add to rooms array
        this.addDataToRooms(options.dataViews[0]);

        // Redraw the map based on the rooms array
        this.drawMap(options.viewport, this.rooms);
    }

    // Parse the dataView and update the rooms object
    private addDataToRooms(dataView: DataView) {
        let self = this;

        // Empty the existing rooms object
        this.rooms.rooms = [];

        /* Because the room attributes (x, y, width, height, color, etc.) can be place in any order, 
            we need to know which columns go with which attributes. */
        let attributes: Attributes = {
            attributes: []
        };

        let columns = dataView.table.columns;
        let rows = dataView.table.rows;

        // Loop through the columns and read the respective attribute names and save them to an array of tuples.
        for (let i = 0; i < columns.length; i++) {
            attributes.attributes.push({
                name: columns[i].displayName,
                index: columns[i].index
            });
        }

        // For every room/row in the table, add that room to the rooms object array.
        for (let i = 0; i < rows.length; i++) {
            // Create an empty "default" room
            let room: Room = {
                name: "",
                category: "",
                position: {
                    x: 0,
                    y: 0
                },
                dimensions: {
                    width: 0,
                    height: 0
                },
                color: "black",
                bedOutsideRoom: false,
                waitingRoom: false,
                numOccupants: 0,
                innerWaitingRoom: false
            };

            // For each column for this room's row in the table, read the attributes.
            attributes.attributes.forEach(function (attribute) {
                let value = rows[i][attribute.index];

                // Based on the name of the attribute for this column, set the appropriate field in the current room object.
                switch (attribute.name) {
                    case "Name":
                        room.name = <string>value
                        break;
                    case "Category":
                        room.category = <string>value;
                        break;
                    case "PosX":
                        room.position.x = <number>value;
                        break;
                    case "PosY":
                        room.position.y = <number>value;
                        break;
                    case "Width":
                        room.dimensions.width = <number>value;
                        break;
                    case "Height":
                        room.dimensions.height = <number>value;
                        break;
                    case "Color":
                        room.color = <string>value;
                        break;
                    case "BedOutsideRoom":
                        room.bedOutsideRoom = self.isTrue(value);
                        break;
                    case "WaitingRoom":
                        room.waitingRoom = self.isTrue(value);
                        break;
                    case "NumOccupants":
                        room.numOccupants = <number>value;
                        break;
                    case "InnerWaitingRoom":
                        room.innerWaitingRoom = self.isTrue(value);
                        break;
                    case "Date":
                        break;
                    default:
                        // Print to console if something strange is found.
                        console.log("Found an unknown attribute: " + attribute.name);
                        console.log(rows[i]);
                        break;
                }
            });

            // Add this room to the rooms array.
            this.rooms.rooms.push(room);
        }
    }

    // Parse any value that could be considered "true" and return a true boolean.  Used because Power BI doesn't support booleans.
    private isTrue(value) {
        return (value === true)
            || (value === 1)
            || (value === "true")
            || (value === "True")
            || (value === "TRUE")
            || (value === "t")
            || (value === "T")
            || (value === "1");
    }

    // Pre-existing Power BI function used to parse the settings that you can set in the Power BI Edit view.
    private static parseSettings(dataView: DataView): VisualSettings {
        return <VisualSettings>VisualSettings.parse(dataView);
    }

    /**
     * This function gets called for each of the objects defined in the capabilities files and allows you to select which of the
     * objects and properties you want to expose to the users in the property pane.
     */
    public enumerateObjectInstances(options: EnumerateVisualObjectInstancesOptions):
        VisualObjectInstance[] | VisualObjectInstanceEnumerationObject {
        return VisualSettings.enumerateObjectInstances(this.settings || VisualSettings.getDefault(), options);
    }

    // Create and draw the map based on the array in the rooms object
    private drawMap(viewport: powerbi.IViewport, mapData: RoomMap) {
        let self = this;

        // Clear the view
        d3.select("svg").remove();

        // Create a new empty svg element to cover the widget
        let svgContainer = d3.select("body").select("#sandbox-host").append("svg")
            .attr("width", viewport.width)
            .attr("height", viewport.height);

        // Calculate the size of the map based on the positions and dimensions of the rooms that will be in it
        let originalMapWidth = 0, originalMapHeight = 0;
        mapData.rooms.forEach(function (room) {
            if (room.position && room.dimensions) {
                if (room.position.x + room.dimensions.width > originalMapWidth) {
                    originalMapWidth = room.position.x + room.dimensions.width;
                }
                if (room.position.y + room.dimensions.height > originalMapHeight) {
                    originalMapHeight = room.position.y + room.dimensions.height;
                }
            }
        });

        /* Add a padding equal to a half-room's width on the x and y axes 
            (this is the total padding so it gets divided by 2). */
        originalMapWidth += 1;
        originalMapHeight += 1;

        // Add some padding between each "cell" in the map "grid"
        originalMapWidth *= 1 + this.paddingRatio;
        originalMapHeight *= 1 + this.paddingRatio;

        /* Calculate how much to scale the rooms to fit into the entire size of 
            the widget - but not stretch horizontally or vertically. */
        this.scaleFactor = viewport.width / originalMapWidth;
        if (originalMapHeight * this.scaleFactor > viewport.height) {
            this.scaleFactor = viewport.height / originalMapHeight;
        }

        // Set how much to separate the rooms from each other
        this.offsetFactor = this.scaleFactor * this.paddingRatio;

        // Set the on-screen map dimensions based on the above-calculated scale factor and map size
        let onScreenMapWidth = originalMapWidth * this.scaleFactor;
        let onScreenMapHeight = originalMapHeight * this.scaleFactor;

        /* Calculate how much to translate the map so that it is in the center of the widget,
            taking into account the padding. */
        let mapOffsetX = ((viewport.width / 2) - (onScreenMapWidth / 2)) + (0.5 * (self.scaleFactor + self.offsetFactor));
        let mapOffsetY = ((viewport.height / 2) - (onScreenMapHeight / 2)) + (0.5 * (self.scaleFactor + self.offsetFactor));

        // For each room in the data, draw it
        mapData.rooms.forEach(function (room) {
            /* Create a unique ID for every room based on its name.  Remove all non-ascii 
                characters and whitespace so it is a valid html ID. */
            let roomID = "id" + room.name.replace(/[\u0250-\ue007]/g, 'A').replace(/\s/g, "B");

            // Calculate the position to draw the room at
            let roomX = (room.position.x * (self.scaleFactor + self.offsetFactor)) + mapOffsetX;
            let roomY = (room.position.y * (self.scaleFactor + self.offsetFactor)) + mapOffsetY;

            // Calculate the size of the room
            let roomW = (room.dimensions.width * self.scaleFactor) + (self.offsetFactor * (room.dimensions.width - 2));
            let roomH = (room.dimensions.height * self.scaleFactor) + (self.offsetFactor * (room.dimensions.height - 2));

            /* Calculate the room height that text should use so that it gets vertically centered in
                each room.  If you use the roomH variable, the text will be slightly too high up. */
            let textAdjustmentHeight = room.dimensions.height * (self.scaleFactor + self.offsetFactor);

            // Based on the type of room, draw them in different ways
            if (room.waitingRoom) {
                /* This is a waiting room so it will either just have the title of the department, 
                    the title + number of occupants, or title, number of occupants and a border. */

                /* Add a container svg object that will contain a rectangle 
                    (to represent the room) and a text box for the room name. */
                let g = svgContainer.append("g")
                    .attr("id", roomID)
                    .attr("transform", "translate(" + roomX + "," + roomY + ")");

                // TODO change these next lines - it's basically hard-coding
                /* This is to differentiate between the true waiting rooms (inner and outer 
                    waiting rooms) and the departments that should only show titles. */
                let onlyTitle = !room.name.endsWith("IVR") && !room.name.endsWith("väntrum");
                let border = room.name.endsWith("väntrum");

                // Create the shape of the waiting room (either border or no border)
                g.append("rect")
                    .attr("width", roomW)
                    .attr("height", roomH)
                    .style("fill", "white")
                    .style("stroke", border ? "black" : "white");

                // Define the text and dimensions of the waiting room name text box
                let textBoxData = [{
                    "text": (onlyTitle ? room.name : room.name + ": " + room.numOccupants),
                    "x": 0,
                    "y": 0,
                    "width": roomW,
                    "height": textAdjustmentHeight
                }];

                // Create the text box; set the text behaviour and positioning
                new d3plus.TextBox()
                    .select("#" + roomID)
                    .data(textBoxData)
                    .fontSize(self.scaleFactor * self.titleTextRatio)
                    .overflow(true)
                    .verticalAlign("middle")
                    .textAnchor("middle")
                    .fontColor("black")
                    .render();
            } else {
                if (!room.bedOutsideRoom) {
                    // This is a regular hospital room so it will just be a rectangle with the room name inside.

                    /* Add a container svg object that will contain a rectangle 
                        (to represent the room) and a text box for the room name. */
                    let g = svgContainer.append("g")
                        .attr("id", roomID)
                        .attr("transform", "translate(" + roomX + "," + roomY + ")");

                    // Create the shape of the room with the right color depending on the occupied status
                    g.append("rect")
                        .attr("width", roomW)
                        .attr("height", roomH)
                        .style("fill", room.color)
                        .attr("opacity", room.numOccupants === 0 ? self.vacantRoomOpacity : self.occupiedRoomOpacity);

                    // Define the text and dimensions of the room name text box
                    let textBoxData = [{
                        "text": room.name,
                        "x": 0,
                        "y": 0,
                        "width": roomW,
                        "height": textAdjustmentHeight
                    }];

                    // Calculate a large enough text size that will fit in the room box
                    const largeRoomTextRatio = 1.2;
                    let defaultRoomFontSize = self.scaleFactor * self.textRatio;
                    let trueRoomFontSize = (room.dimensions.width <= 1 ? defaultRoomFontSize : defaultRoomFontSize * largeRoomTextRatio);

                    // Create the text box; set the text behaviour, positioning and color
                    new d3plus.TextBox()
                        .select("#" + roomID)
                        .data(textBoxData)
                        .fontSize(trueRoomFontSize)
                        .overflow(true)
                        .verticalAlign("middle")
                        .textAnchor("middle")
                        .fontColor(room.numOccupants === 0 ? self.vacantRoomTextColor : self.occupiedRoomTextColor)
                        .render();
                } else {
                    // This is a hospital bed outside a regular hospital room so it will show up as a colored dot on the map.

                    // For every "extra bed" in the current department, draw a dot on the map (up to four (4) supported)
                    for (let i = 1; i < room.numOccupants + 1; i++) {
                        // Calculate the position of this bed on the map so the dots show up in a square formation
                        let xOffset = ((i + 1) % 2) * 2 - 1;
                        let yOffset = (i > 2 ? 1 : 0) * 2 - 1;

                        // Add this dot to the map with the correct position, radius and color
                        svgContainer.append("circle")
                            .attr("cx", roomX + (roomW / 2) + (xOffset * (roomW / 4)))
                            .attr("cy", roomY + (roomH / 2) + (yOffset * (roomH / 4)))
                            .attr("r", roomW / 5)
                            .attr("fill", room.color);
                    }
                }
            }
        });
    }
}