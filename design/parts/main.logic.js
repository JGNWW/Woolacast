class Component extends DCLogic {
  constructor(props) {
    super(props);
    this.state = { source: 'apple', level: 'shows' };
  }

  data() {
    return {
      apple: {
        name: 'Apple Podcasts', initial: 'A',
        updated: 'Bijgewerkt 06:00', cat: 'Alle categorieën', catOn: true,
        shows: [
          ['De Deadline', 'Bureau Kruit', 'a1', 1],
          ['Koud Spoor', 'Podium Audio', 'a7', -1],
          ['Nachtdienst', 'VRIJDAG Media', 'a2', 4],
          ['Tafel voor Twee', 'Roos & Van Dijk', 'a3', 0],
          ['Vandaag in Zeven', 'Dagblad Noord', 'a9', 2],
          ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 'new'],
          ['Lange Adem', 'Studio Hemel', 'a5', -3]
        ],
        episodes: [
          ['De laatste getuige (deel 3)', 'Koud Spoor · 54 min · 9 sep', 'a7', 'new'],
          ['Wat er misging in Den Haag', 'Vandaag in Zeven · 22 min · 10 sep', 'a9', 3],
          ['Live vanuit Paradiso', 'De Deadline · 71 min · 8 sep', 'a1', -1],
          ['Zes maanden nachtploeg', 'Nachtdienst · 46 min · 7 sep', 'a2', 2],
          ['Het duurste half uur', 'Kade 12 · 33 min · 9 sep', 'a10', 0],
          ['Aflevering 88: thuiskomen', 'Tafel voor Twee · 58 min · 6 sep', 'a3', -2]
        ]
      },
      spotify: {
        name: 'Spotify', initial: 'S',
        updated: 'Bijgewerkt 05:30', cat: 'Alle categorieën', catOn: true,
        shows: [
          ['Nachtdienst', 'VRIJDAG Media', 'a2', 0],
          ['De Deadline', 'Bureau Kruit', 'a1', 2],
          ['Ondergronds', 'Kelderwerk', 'a4', 5],
          ['Zwart op Wit', 'Concept Media', 'a8', -2],
          ['Koud Spoor', 'Podium Audio', 'a7', -1],
          ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 9],
          ['Kade 12', 'Kade Media', 'a10', 'new']
        ],
        episodes: [
          ['Nachtploeg #212: de brug', 'Nachtdienst · 41 min · 10 sep', 'a2', 4],
          ['Waarom niemand meer belt', 'Ondergronds · 29 min · 9 sep', 'a4', 'new'],
          ['De laatste getuige (deel 3)', 'Koud Spoor · 54 min · 9 sep', 'a7', -1],
          ['Het duurste half uur', 'Kade 12 · 33 min · 9 sep', 'a10', 6],
          ['Wie betaalt de rekening?', 'Zwart op Wit · 37 min · 8 sep', 'a8', -2],
          ['Seizoensfinale', 'De Deadline · 64 min · 5 sep', 'a1', -3]
        ]
      },
      youtube: {
        name: 'YouTube', initial: 'Y',
        updated: 'Week 37 · wo 8 sep', cat: 'Geen categorieën', catOn: false,
        shows: [
          ['De Deadline', 'Bureau Kruit', 'a1', 0],
          ['Het Vijfde Kwartier', 'Sportcast NL', 'a6', 3],
          ['Ondergronds', 'Kelderwerk', 'a4', 1],
          ['Tafel voor Twee', 'Roos & Van Dijk', 'a3', -2],
          ['Nachtdienst', 'VRIJDAG Media', 'a2', -1],
          ['Zwart op Wit', 'Concept Media', 'a8', 'new']
        ],
        episodes: null
      }
    };
  }

  move(m) {
    if (m === 'new') return { mvText: 'NIEUW', mvCls: 'new', mvColor: '#C4542B', up: false, down: false };
    if (m > 0) return { mvText: String(m), mvCls: '', mvColor: '#0E8A4E', up: true, down: false };
    if (m < 0) return { mvText: String(-m), mvCls: '', mvColor: '#C03A24', up: false, down: true };
    return { mvText: '–', mvCls: 'flat', mvColor: '#988C7A', up: false, down: false };
  }

  renderVals() {
    const d = this.data();
    const cur = d[this.state.source];
    const list = this.state.level === 'shows' ? cur.shows : cur.episodes;

    const sources = ['apple', 'spotify', 'youtube'].map((k) => ({
      name: d[k].name,
      initial: d[k].initial,
      cls: this.state.source === k ? 'on' : '',
      pick: () => this.setState({ source: k })
    }));

    const levels = [['shows', 'Podcasts'], ['episodes', 'Afleveringen']].map(([k, label]) => ({
      name: label,
      cls: this.state.level === k ? 'on' : '',
      pick: () => this.setState({ level: k })
    }));

    const rows = (list || []).map((r, i) => Object.assign({
      rank: i + 1,
      rankCls: i < 3 ? 'top' : '',
      title: r[0],
      sub: r[1],
      art: r[2]
    }, this.move(r[3])));

    return {
      sources,
      levels,
      rows,
      empty: !list,
      ctx: {
        country: 'Nederland',
        cat: cur.cat,
        catStyle: cur.catOn ? 'color:#6E6458' : 'color:#988C7A;text-decoration:line-through',
        updated: cur.updated
      },
      toApple: () => this.setState({ source: 'apple' })
    };
  }
}
