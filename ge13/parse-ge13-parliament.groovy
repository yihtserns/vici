@Grab('org.ccil.cowan.tagsoup:tagsoup:1.2.1')
import org.ccil.cowan.tagsoup.Parser
import groovy.util.XmlSlurper
import java.text.NumberFormat
import groovy.json.JsonBuilder

def folder = new File(args[0])
def parser = new XmlSlurper(new Parser())

def states = folder.listFiles().grep { it.isFile() }.collect { file ->
  def state = State.from(file)
  System.err.println 'Resolving constituents for state: ' + state.name

  def constituentsHtml = parser.parse(file)
  def constituents = constituentsHtml.body.form.table.tbody.tr.td.collect { Constituent.from(it) }.grep()
  constituents.each { constituent ->
    System.err.println "Resolving result for constituent '$constituent.name'..."
    String result = constituent.resultUrl().toURL().text
    def candidatesHtml = parser.parseText(result)
    def rows = candidatesHtml.body.table.tr[2].td.table.tr;
    def candidates = rows[1..-2].collect { Candidate.from(it) }
    def stat = Stat.from(rows[-1].td.table.tr);

    constituent.candidates = candidates
    constituent.stat = stat;
    state.constituents << constituent
  }

  return state
}

println new JsonBuilder(states).toPrettyString()
System.err.println 'Success'

class State {
  def name
  def constituents = []

  static State from(File file) {
    String stateName = file.name.substring(0, file.name.lastIndexOf('.'))

    return new State(name: stateName)
  }
}

class Constituent {
  String id
  String name
  def candidates
  def stat

  String resultUrl() {
    'http://resultpru13.spr.gov.my/module/keputusan/paparan/5_KeputusanDR.php?kod=' + id
  }

  static Constituent from(td) {
    String areaName = td.text().replace('\u00A0', '').trim()
    if (areaName == '') {
      return null
    }
    String areaId = (td.a.@onclick =~ /windKeputusan\('5_KeputusanDR.php', '(\d+)'\)/)[0][1]

    return new Constituent(id: areaId, name: areaName)
  }
}

class Candidate {
  String name
  String party
  int voteCount

  public static Candidate from(tr) {
    new Candidate(
      name: tr.td[0].text().trim(),
      party: tr.td[1].text().trim(),
      voteCount: NumberFormat.getInstance(Locale.US).parse(tr.td[2].text().trim()))
  }
}

class Stat {
  int registeredVoterCount;
  int rejectedBallotCount;
  int castedBallotCount;
  int releasedBallotCount;
  int unreturnedBallotCount;

  static Stat from(tr) {
    new Stat(
      registeredVoterCount: NumberFormat.getInstance(Locale.US).parse(tr[0].td[3].text().trim()),
      rejectedBallotCount: NumberFormat.getInstance(Locale.US).parse(tr[1].td[3].text().trim()),
      castedBallotCount: NumberFormat.getInstance(Locale.US).parse(tr[2].td[3].text().trim()),
      releasedBallotCount: NumberFormat.getInstance(Locale.US).parse(tr[3].td[3].text().trim()),
      unreturnedBallotCount: NumberFormat.getInstance(Locale.US).parse(tr[4].td[3].text().trim()));
  }
}
