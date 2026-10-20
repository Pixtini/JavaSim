#!/usr/bin/env python3
"""Check retained G002 reports, replay every saved round, and summarize sampling uncertainty."""
import csv
import hashlib
import io
import json
import math
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
exact = {r['metric']: float(r['value']) for r in csv.DictReader((ROOT / 'exact-math.csv').open())}
summary = []
replay_log = io.StringIO()
for folder in sorted((ROOT / 'runs').glob('*/*')):
    if not folder.is_dir():
        continue
    text = (folder / 'simulation_stats.txt').read_text()
    mode = folder.parent.name.split('-')[0]
    def value(key, section=text):
        match = re.search(r'^' + re.escape(key) + r': (.+)$', section, re.M)
        if not match:
            raise AssertionError('Missing ' + key)
        return float(match[1].replace('£', '').replace('%', ''))
    rounds = int(value('Rounds'))
    triggers = int(value('FG Triggers'))
    rtp = value('Simulated RTP') / 100
    cost = {'Normal': 1, 'EM1': 2, 'BB1': 47}[mode]
    assert abs(value('Total staked') - rounds * cost) < .01
    base_section = text.split('\nBasegame\n')[1].split('\nBasegame Set')[0]
    free_section = text.split('\nFreegame\n')[1].split('\nFreegame Set')[0]
    assert int(value('Rounds', free_section)) == triggers * 8
    assert abs(value('Total winnings') - value('Total winnings', base_section)
               - value('Total winnings', free_section)) <= .151
    assert value('Wincaps') == 0  # This observation is not proof that the maximum is impossible.
    counts = {}
    with (folder / 'win_distributions.csv').open() as source:
        for row in csv.DictReader(source):
            if row['Distribution']:
                counts[row['Distribution']] = counts.get(row['Distribution'], 0) + int(row['Hits'])
    assert counts == {'Total Game': rounds, 'Basegame': rounds, 'Freegame': triggers}
    workbooks = list(folder.glob('*.xlsx'))
    assert len(workbooks) == 1 and (folder / 'config.html').is_file()
    with (folder / 'saved_gameplays.csv').open() as source:
        saved = list(csv.DictReader(source))
    assert all(row['Game Mode'] == mode for row in saved)
    ids = sorted({int(row['Gameplay ID']) for row in saved})
    assert len(ids) == 10
    for gameplay_id in ids:
        result = subprocess.run(['java', '-Xmx256m', '-cp', str(REPO / 'build/classes'),
            'toolkit.replay.ReplayerMain', str(folder / 'saved_gameplays.csv'), str(gameplay_id)],
            cwd=REPO, capture_output=True, text=True, check=True)
        assert 'Replay validation: PASSED' in result.stdout
        replay_log.write(str(folder.relative_to(ROOT)) + ' id=' + str(gameplay_id) + '\n' + result.stdout)
    theoretical = exact[mode + '_rtp']
    standard_error = exact[mode + '_round_std_base_units'] / (cost * math.sqrt(rounds))
    trigger_p = exact[mode + '_trigger']
    trigger_se = math.sqrt(rounds * trigger_p * (1 - trigger_p))
    summary.append(dict(mode=mode, run=folder.parent.name, rounds=rounds, expected_rtp=theoretical,
        observed_rtp=rtp, ci95_low=rtp - 1.96 * standard_error, ci95_high=rtp + 1.96 * standard_error,
        rtp_z=(rtp - theoretical) / standard_error, triggers=triggers,
        expected_triggers=rounds * trigger_p,
        trigger_z=(triggers - rounds * trigger_p) / trigger_se if trigger_se else 0,
        replays=len(ids), report=str(folder.relative_to(ROOT))))
with (ROOT / 'run-summary.csv').open('w') as output:
    writer = csv.DictWriter(output, fieldnames=list(summary[0]), lineterminator="\n"); writer.writeheader(); writer.writerows(summary)
(ROOT / 'report-replays.txt').write_text(replay_log.getvalue())
files = [p for p in (ROOT.parent / 'source').iterdir() if p.suffix in ('.xlsx', '.json', '.py')]
files += list((ROOT.parent / 'src').rglob('*.java'))
files += [REPO / p for p in ['game/GameFactory.java', 'ToolkitGUI.java', 'toolkit/player/EmberForgePlayerAdapter.java',
          'toolkit/player/EmberForgePlayerMain.java', 'toolkit/player/PlayerAdapterFactory.java',
          'toolkit/player/GameLauncherFrame.java', 'toolkit/viewer/WinScreenPrinter.java']]
manifest = {'status': 'rough prototype, no production approval', 'date': '2026-10-20',
    'git_head': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
    'working_tree': 'Uncommitted G002 additions and integration edits; hashes identify exact files.',
    'java': subprocess.run(['java', '-version'],capture_output=True,text=True).stderr.strip(),
    'settings': {'stake': 1, 'threads': 2, 'partitions': 16, 'maxSavedGameplays': 10,
        'seeds': {'Normal': 440055, 'EM1': 440056, 'BB1': 440057}},
    'files_sha256': {str(p.relative_to(REPO)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}}
(ROOT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
print('Verified', len(summary), 'reports and', sum(r['replays'] for r in summary), 'exported-workbook replays.')
for row in summary:
    if 'balance' in row['run']:
        print(row['mode'], 'RTP', f"{100*row['observed_rtp']:.4f}%", 'z', f"{row['rtp_z']:.3f}",
              'triggers', row['triggers'], 'trigger z', f"{row['trigger_z']:.3f}")
