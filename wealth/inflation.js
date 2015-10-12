var vici = (function() {
  var year2InflationRate = JSON.parse($.ajax({
    url: 'inflation-rates.json',
    mimeType: 'text/plain',
    async: false
  }).responseText);

  var year2RelativeCpi = (function() {
    var years = Object.keys(year2InflationRate).sort().reverse();

    var year2Cpi = {};
    _.each(years, function(year, i) {
      if (i === 0) {
        year2Cpi[year] = 1;
      } else {
        var nextYear = years[i - 1];
        var cpi = year2Cpi[nextYear]/(year2InflationRate[nextYear]/100 + 1);
        year2Cpi[year] = cpi;
      }
    });

    return year2Cpi;
  })();

  return {
    wealth: {
      inflation: {
        calculateInflationAdjustedValue: function(year, value) {
          var cpi = year2RelativeCpi[year];

          return (value / cpi);
        }
      }
    }
  };
})();
