var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 900 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var parseDate = d3.time.format("%d-%b-%y").parse;
var data = [
            [
             {date:parseDate("1-May-12"),value:500},
             {date:parseDate("3-May-12"),value:450},
             {date:parseDate("5-May-12"),value:600}
            ],
            [
              {date:parseDate("2-May-12"),value:400},
              {date:parseDate("3-May-12"),value:200}
            ],
            [
             {date:parseDate("1-May-12"),value:400},
             {date:parseDate("5-May-12"),value:200}
            ]
           ];


var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

// add scale of the axes
x.domain(d3.extent(data[0], function(d) { return d.date; }));
y.domain([0, d3.max(data.map(function(d) { return d3.max(d, function(dm) { return dm.value; }); } )) * 1.1]);

var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");

var line = d3.svg.line()
    .x(function(d) { return x(d.date); })
    .y(function(d) { return y(d.value); });

var svg = d3.select("body").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

var lines = svg.selectAll("g").data(data);

//for each array, create a 'g' line container
var aLineContainer = lines
  .enter().append("g"); 

aLineContainer.append("path")
.attr("class", "line")
.attr("d", line);

svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);

svg.append("g")
    .attr("class", "y axis")
    .call(yAxis)
  .append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 6)
    .attr("dy", ".71em")
    .style("text-anchor", "end")
    .text("Price ($)");