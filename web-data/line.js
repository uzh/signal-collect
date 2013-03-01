/*
 * TODO
 * - add tool tips
 * - add auto update (with animation)
 * - add lines for setting up, graph building, calculation
 */

var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 900 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var parseDate = d3.time.format("%d-%b-%y").parse;
var data = [
            [
             {date:parseDate("1-May-12"), value:500, id:"average"},
             {date:parseDate("3-May-12"), value:450, id:"average"},
             {date:parseDate("5-May-12"), value:600, id:"average"}
            ],
            [
              {date:parseDate("2-May-12"), value:400, id:"Node1"},
              {date:parseDate("3-May-12"), value:200, id:"Node1"}
            ],
            [
             {date:parseDate("1-May-12"), value:400, id:"Node1"},
             {date:parseDate("5-May-12"), value:200, id:"Node1"}
            ]
           ];


var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

// add scale of the axes
x.domain(d3.extent(data[0], function(d) { return d.date; }));
x.domain([parseDate("30-Apr-12"), parseDate("6-May-12")]);
y.domain([0, d3.max(data.map(function(d) { return d3.max(d, function(dm) { return dm.value; }); } )) * 1.1]);

var xAxis = d3.svg.axis()
    .scale(x)
    // add ticks (axis and vertical line)
    .tickSize(-height).tickPadding(6).ticks(5)
    .orient("bottom");

var tickFormatY = d3.format("s"); // add SI-postfix (like 2k instead of 2000)
var yAxis = d3.svg.axis()
    .scale(y)
    // add ticks (axis and vertical line)
    .tickSize(-width).tickFormat(tickFormatY).tickPadding(6).ticks(5)
    .orient("left");

var line = d3.svg.line()
    .x(function(d) { return x(d.date); })
    .y(function(d) { return y(d.value); });

var zoom = d3.behavior.zoom().x(x)
            .scaleExtent([0.2, 5]) // allow zooming in/out five times each
            .on("zoom", draw);

var svg = d3.select("body").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")")
    .call(zoom);

// needed for zooming and dragging
var rect = svg.append("rect").attr("width", width).attr("height", height);

// avoid data lines to overlap with axis
var svgBox = svg.append("svg").attr("width", width).attr("height", height)
                .attr("viewBox", "0 0 " + width + " " + height);

var lines = svgBox.selectAll("g").data(data);

//for each array, create a 'g' line container
var aLineContainer = lines
  .enter().append("g");

aLineContainer.append("path").attr("class", "line");

svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")");

svg.append("g")
    .attr("class", "y axis")
  .append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 6)
    .attr("dy", ".71em")
    .style("text-anchor", "end")
    .text("Price ($)");

// show scatter points and tooltips
var formatTime = d3.time.format("%e %B");
var div = d3.select("body").append("div").attr("class", "tooltip").style("opacity", 0);
aLineContainer.selectAll(".dot")
.data( function(d, i) { return d; } )  // This is the nested data call
.enter()
  .append("circle")
  .attr("class", "dot")
  .attr("r", 3.5)
  .on("mouseover", function(d) {      
            div.transition()        
               .duration(100)      
               .style("opacity", .9);      
            div.html(formatTime(d.date) + "<br/>"  + d.value + "<br/>"  + d.id)  
               .style("left", (d3.event.pageX) + "px")     
               .style("top", (d3.event.pageY - 28) + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });

draw();

function draw() {
  svg.select("g.x.axis").call(xAxis);
  svg.select("g.y.axis").call(yAxis);
//  svg.select("path.area").attr("d", area);
  svg.selectAll("path.line").attr("d", line);
  aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
//  d3.select("#footer span").text("U.S. Commercial Flights, " + x.domain().map(format).join("-"));
}