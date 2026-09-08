"""Compile and check all reviewed Java/C++ references against seeded expected values.
This is a development test harness, not an alternative application judge.
"""
import json,re,subprocess,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'target/editorial-reference-checks';OUT.mkdir(parents=True,exist_ok=True)
string=r'"(?:\\.|[^"\\])*"'
records=[]
for line in (ROOT/'src/main/java/com/leetcode/backend/config/VerdixaProblemCatalogSeeder.java').read_text().splitlines():
 if not line.strip().startswith('s("'):continue
 values=[json.loads(v) for v in re.findall(string,line.split(',cs(')[0])]
 title,*_=values;name,returns=values[8:10];types=values[11::2]
 cases=[(json.loads(json.loads(a)),json.loads(json.loads(b))) for a,b in re.findall(r'[ph]\(('+string+r'),('+string+r')\)',line)]
 records.append((name,returns,types,cases))
records.extend([('reverseString','String',['String'],[(['abc'],'cba'),([''],''),(['racecar'],'racecar')]),('twoSum','int[]',['int[]','int'],[([[2,7,11,15],9],[0,1]),([[3,3],6],[0,1]),([[-1,-2,-3,-4,-5],-8],[2,4])]),('isValid','boolean',['String'],[(['([])'],True),(['([)]'],False),([''],True)])])
def literal(value,type,lang):
 if type.endswith('[]'):
  base=type[:-2];return ('new '+type if lang=='java' else 'vector<'+('string' if base=='String' else base)+'>')+'{'+','.join(literal(v,base,lang) for v in value)+'}'
 if type=='String':return json.dumps(value)
 if type=='boolean':return str(value).lower()
 return str(value)+('L' if type=='long' and lang=='java' else 'LL' if type=='long' else '')
for lang in ['java','cpp']:
 methods=(ROOT/f'tools/editorial_references.{lang}').read_text()
 checks=[];count=0
 for name,returns,types,cases in records:
  for args,expected in cases:
   call=('s.' if lang=='java' else '')+name+'('+','.join(literal(v,t,lang) for v,t in zip(args,types))+')'
   expect=literal(expected,returns,lang)
   condition=('java.util.Arrays.equals('+call+','+expect+')' if returns.endswith('[]') else 'java.util.Objects.equals('+call+','+expect+')') if lang=='java' else '('+call+'=='+expect+')'
   checks.append(('if(!'+condition+')throw new AssertionError("'+name+' case '+str(count)+'");') if lang=='java' else ('if(!'+condition+'){cerr<<"'+name+' case '+str(count)+'";return 1;}'))
   count+=1
 if lang=='java':
  path=OUT/'EditorialReferenceCheck.java';path.write_text('import java.util.*;\npublic class EditorialReferenceCheck {\n'+methods+'\npublic static void main(String[] args){var s=new EditorialReferenceCheck();\n'+'\n'.join(checks)+'\nSystem.out.println("'+str(count)+' Java cases passed");}}')
  subprocess.run(['javac',str(path)],check=True);subprocess.run(['java','-cp',str(OUT),'EditorialReferenceCheck'],check=True)
 else:
  path=OUT/'references.cpp';path.write_text('#include <bits/stdc++.h>\nusing namespace std;\n'+methods+'\nint main(){\n'+'\n'.join(checks)+'\ncout<<"'+str(count)+' C++ cases passed";}')
  compiler=os.environ.get('ALGOSPHERE_CPP_COMMAND',r'C:\msys64\ucrt64\bin\g++.exe' if os.name=='nt' else 'g++');binary=OUT/('references.exe' if os.name=='nt' else 'references')
  env=os.environ.copy();env['PATH']=str(Path(compiler).parent)+os.pathsep+env['PATH']
  subprocess.run([compiler,'-std=c++17','-O2',str(path),'-o',str(binary)],check=True,env=env)
  subprocess.run([str(binary)],check=True,env=env)
