/*
 * TODO
 * - add update animation
 */

var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 900 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var now = new Date;
var interval = 2000;

var parseDate = d3.time.format("%d-%b-%y").parse;
var data = [
            [
             {date:now, value:500, id:"average"}
            ],
            [
              {date:now, value:400, id:"Node1"}
            ],
            [
             {date:now, value:400, id:"Node1"}
            ]
           ];


var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

// add scale of the axes
//x.domain(d3.extent(data[0], function(d) { return d.date; }));
//x.domain([nowTimestamp-60, nowTimestamp+60]);
x.domain([new Date(+(now)-(10*1000)), new Date(+(now)+(120*1000))]);
y.domain([0, 1]);

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

var currentDate = now;
function update() {
  console.log("updating");
  
  currentDate = new Date(+(currentDate)+interval);
  
  var newData = [
                 {date:currentDate, value:(Math.random()+1)*200, id:"average"},
                 {date:currentDate, value:(Math.random()+1)*100, id:"min"},
                 {date:currentDate, value:(Math.random()+2)*200, id:"max"},
                ];
  
  var lowestXDomain      = x.domain()[0];
  var highestXDomain     = x.domain()[1];
  var currentHighestDate = d3.max(data[0], function(d) { return d.date });
  
  // is current highest date currently being showed?
  if (lowestXDomain <= currentHighestDate && currentHighestDate <= highestXDomain) {
    var newHighestDate = d3.max(newData, function(d) { return d.date });
    // if new highest date is out of the domain, update the domain
    if (highestXDomain < newHighestDate) {
      x.domain([new Date(+(lowestXDomain)+(interval)), newHighestDate]);
      console.log("update x domain");
//      svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);
    }
  }
  
  newData.forEach(function(d, i) {
    data[i].push(d);
  });
//  console.dir(data);
  
  aLineContainer
  .attr("d", line)
  .attr("transform", null)
//  .transition()
//  .duration(500)
//  .ease("linear")
//  .attr("transform", "translate(" + x(0) + ")")
//  .each("end", update);
  
//  d3.selectAll("path.line").data({date:parseDate("4-May-12"), value:Math.random()*500, id:"Node1"}).enter();

  // update domains
//  x.domain([parseDate("30-Apr-12"), parseDate("6-Jun-12")]);
  y.domain([0, d3.max(data.map(function(d) { return d3.max(d, function(dm) { return dm.value; }); } )) * 1.1]);
//  xAxis.scale(x);
//  yAxis.scale(y);
  

  
//  lines = svgBox.selectAll("g").data([newData]);
//  lines.enter().append("g");
  
  draw();
}

window.setInterval(function() {
  update();
}, interval);


draw();

function draw() {
  svg.select("g.x.axis").call(xAxis);
  svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);

  
  //  svg.select("path.area").attr("d", area);
  svg.selectAll("path.line").attr("d", line);
  aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
//  d3.select("#footer span").text("U.S. Commercial Flights, " + x.domain().map(format).join("-"));
}