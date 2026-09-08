"""Build reviewed editorial resources from repository contracts, with seeded-case checks.
Run from the repository root: python backend/tools/build_editorials.py
"""
import ast, json, re, importlib.util
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'src/main/java/com/leetcode/backend/config/VerdixaProblemCatalogSeeder.java').read_text()
references=(ROOT/'tools/editorial_references.py').read_text()
functions={node.name:ast.get_source_segment(references,node) for node in ast.parse(references).body if isinstance(node,ast.FunctionDef)}
namespace={};exec(references,namespace)
notes={parts[0]:parts[1:] for parts in (line.split('|') for line in (ROOT/'tools/editorial_notes.txt').read_text().splitlines())}
string=r'"(?:\\.|[^"\\])*"'
entries=[];tests=0
for line in source.splitlines():
    if not line.strip().startswith('s("'):continue
    values=[json.loads(v) for v in re.findall(string,line.split(',cs(')[0])]
    title,difficulty,tags,description,constraints,input_format,output,examples,name,returns,*params=values
    py_name=re.sub(r'(?<!^)(?=[A-Z])','_',name).lower()
    hints=[json.loads(v) for v in re.findall(string,line.split(',hs(')[1])]
    if name == 'missingBeacon':
        hints=['Every identifier except the missing one occurs in both the complete range and the input.', 'XOR is associative and x XOR x is zero, so paired identifiers cancel without sum overflow.', 'Initialize answer to n; for i from 0 to n-1 XOR answer with i and nums[i]. Return the remaining identifier.']
    elif name == 'palindromeMinimumCuts':
        hints=['An optimal prefix partition ends in a palindromic suffix.', 'Mark pal[left][right] when endpoints match and the interior is palindromic; initialize cuts[length] = length - 1.', 'Process right endpoints in increasing order. For every palindromic s[left..right], minimize cuts[right+1] with cuts[left]+1. Return cuts[n], or 0 for the empty string.']
    elif name == 'queenBoardCounter':
        hints=['Place one queen per row; track occupied columns and the two attacking diagonals.', 'Represent occupied columns and diagonals as bit masks. Legal columns are the board mask with all attacked bits removed.', 'Extract each legal low bit, recurse with the occupied column and shifted diagonal masks, and sum completions. Masks are passed by value so sibling branches cannot interfere.']
    cases=re.findall(r'[ph]\(('+string+r'),('+string+r')\)',line)
    for arguments,expected in cases:
        args=json.loads(json.loads(arguments));expect=json.loads(json.loads(expected));actual=namespace[py_name](*args)
        assert actual==expect,(title,args,actual,expect)
        tests+=1
    time,space,proof,edges=notes[title]
    guidance=f'FUNCTION mode: implement {name}({", ".join(params[::2])}) returning {returns}. In Python the wrapper calls {py_name}. Use the provided method/function, with no main method, input parsing, or printing. Java imports java.util.* and C++ uses standard containers. '+input_format+' '+output
    entries.append(dict(problemTitle=title,functionName=name,returnType=returns,parameterTypes=params[1::2],title=title+' — explained',intuition=hints[0],keyObservations=description+'\n'+hints[1],approach=hints[-1],algorithmExplanation='\n'.join(f'{i+1}. {h}' for i,h in enumerate(hints))+'\n4. Return the result using the exact contract: '+output,correctness=proof,timeComplexity=time,spaceComplexity=space,edgeCases=edges,referenceGuidance=guidance,pythonSolution=functions[py_name]))
for title,name,returns,params,py_name,intuition,approach in [
 ('Reverse String','reverseString','String',['String'],'reverse_string','Reversal maps each position to its mirror around the center.','Read characters from the last position to the first, collecting them in a new string.'),
 ('Two Sum Updated','twoSum','int[]',['int[]','int'],'two_sum','For each number, the needed partner is target minus that number.','Keep a hash map of earlier values and indices. Look up the complement before inserting the current value.'),
 ('Valid Parentheses','isValid','boolean',['String'],'is_valid','Nested pairs must close in the reverse order they opened.','Push opening brackets on a stack. Each closing bracket must match the stack top; require an empty final stack.')]:
 time,space,proof,edges=notes[title]
 entries.append(dict(problemTitle=title,functionName=name,returnType=returns,parameterTypes=params,title=title+' — explained',intuition=intuition,keyObservations=proof,approach=approach,algorithmExplanation='1. Initialize the state described in the approach.\n2. '+approach+'\n3. Return the result, without printing or reading stdin.',correctness=proof,timeComplexity=time,spaceComplexity=space,edgeCases=edges,referenceGuidance=f'FUNCTION mode: implement {name}; Python uses {py_name}. Return {returns}. Preserve the supplied signature and do not add a main function.',pythonSolution=functions[py_name]))
# Other language references are maintained alongside these source contracts.
for language in ['java','cpp']:
 path=ROOT/f'tools/editorial_references.{language}'
 if path.exists():
  code={re.search(r'(\w+)\(',line).group(1):line for line in path.read_text().splitlines() if line and not line.startswith('//')}
  for e in entries:e[language+'Solution']=code[e['functionName']]
(ROOT/'src/main/resources/editorials/catalog.json').write_text(json.dumps(entries,indent=2,ensure_ascii=False),encoding='utf-8')
print(f'{len(entries)} editorials; {tests} Python seeded cases passed')
