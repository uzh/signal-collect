var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 900 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var now = new Date;
var interval = 1000;

var data = [
            [
             {date:now, value:300, id:"average"}
            ],
            [
              {date:now, value:150, id:"Node1"}
            ],
            [
             {date:now, value:500, id:"Node1"}
            ]
           ];


// add ranges
var x = d3.time.scale().range([0, width]);
var y = d3.scale.linear().range([height, 0]);

// add default scale of the axes
x.domain([new Date(+(now)-(5*1000)), new Date(+(now)+(120*1000))]);
y.domain([0, 1]);

var xAxis = d3.svg.axis().scale(x)
    // add ticks (axis and vertical line)
    .tickSize(-height).tickPadding(6).ticks(5).orient("bottom");

var tickFormatY = d3.format("s"); // add SI-postfix (like 2k instead of 2000)
var yAxis = d3.svg.axis().scale(y)
    // add ticks (axis and vertical line)
    .tickSize(-width).tickFormat(tickFormatY).tickPadding(6).ticks(5).orient("left");

var line = d3.svg.line()
    .x(function(d) { return x(d.date); })
    .y(function(d) { return y(d.value); });

var zoom = d3.behavior.zoom().x(x)
            .scaleExtent([0.005, 5]) // allow zooming in/out
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
var aLineContainer = lines.enter().append("g");
var path = aLineContainer.append("path").attr("class", "line");

// add x axis to chart
svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")");

// add y axis to chart
svg.append("g")
    .attr("class", "y axis")
  .append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 6)
    .attr("dy", ".71em")
    .style("text-anchor", "end")
    .text("Price ($)");

// show scatter points and tool tips
var formatTime = d3.time.format("%Y-%m-%d %H:%M:%S");
var div = d3.select("body").append("div").attr("class", "tooltip").style("opacity", 0);

var currentDate = now;
function update() {
  console.log("updating");
  
  currentDate = new Date(+(currentDate)+interval);
  
  var newData = [
                 {date:currentDate, value:(Math.random()+1)*200, id:"average"},
                 {date:currentDate, value:(Math.random()+1)*100, id:"min"},
                 {date:currentDate, value:(Math.random()+2)*200, id:"max"},
                ];
  
  var shiftRight         = false;
  var lowestXDomain      = x.domain()[0];
  var highestXDomain     = x.domain()[1];
  var currentHighestDate = d3.max(data[0], function(d) { return d.date });
  
  // is current highest date currently being showed?
  if (lowestXDomain <= currentHighestDate && currentHighestDate <= highestXDomain) {
    var newHighestDate = d3.max(newData, function(d) { return d.date });
    // if new highest date is out of the domain, update the domain
    if (highestXDomain < newHighestDate) {
      shiftRight = true;
//      svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);
    }
  }

  newData.forEach(function(d, i) {
    data[i].push(d);
  });
  
  path.attr("d", line).attr("transform", null);
  
  // only perform animated transition when needed or we will have problems when dragging/zooming
  d3.transition().ease("linear").duration((shiftRight ? interval : 0)).each(function() {

    if (shiftRight) {
      // update x domain
      x.domain([new Date(+(lowestXDomain)+(interval)), newHighestDate]);
      zoom.x(x);
      
      // line transition
      var transformVal = new Date(+(currentDate) - (+(x.domain()[1])-(+(x.domain()[0])) + interval));
      path.transition()
          .ease("linear")
          .attr("transform", "translate(" + x(transformVal) + ")");
    }
    
    // update scatter points
    aLineContainer.selectAll(".dot")
      .data( function(d, i) { return d; } )  // This is the nested data call
      .enter()
        .append("circle")
        .attr("class", "dot")
        .attr("r", 6)
        .on("mouseover",
            function(d) {
              div.transition()        
                 .duration(100)      
                 .style("opacity", .9);      
              div.html(formatTime(d.date) + "<br/>"  + d.value + "<br/>"  + d.id)  
                 .style("left", (d3.event.pageX) + "px")     
                 .style("top", (d3.event.pageY - 28) + "px");    
            })                  
            .on("mouseout",
                function(d) {       
                  div.transition()        
                     .duration(500)      
                     .style("opacity", 0);   
                });
    
    // update x domains
    y.domain([0, d3.max(data.map(function(d) { return d3.max(d, function(dm) { return dm.value; }); } )) * 1.1]);
  
    draw();
  });

}

window.setInterval(function() {
  update();
}, interval);


draw();

function draw() {
  svg.select("g.x.axis").call(xAxis);
  svg.select("g.y.axis").transition().duration(300).ease("linear").call(yAxis);

  svg.selectAll("path.line").attr("d", line);
  aLineContainer.selectAll("circle.dot").attr("cx", line.x()).attr("cy", line.y());
//  d3.select("#footer span").text("U.S. Commercial Flights, " + x.domain().map(format).join("-"));
}