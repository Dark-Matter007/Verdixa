export default function EditorialContent({ editorial }) {
  if (!editorial) return null;
  return <div className="editorial-content"><h2>{editorial.title}</h2>
    {[["intuition","Intuition"],["keyObservations","Key Observations"],["approach","Approach"],["algorithmExplanation","Step-by-step Explanation"],["correctness","Correctness"],["edgeCases","Edge Cases"],["referenceGuidance","Reference implementation guidance"]].map(([key,label]) => editorial[key] && <section key={key}><h3>{label}</h3><p>{editorial[key]}</p></section>)}
    <section><h3>Complexity</h3><p><strong>Time:</strong> {editorial.timeComplexity} · <strong>Space:</strong> {editorial.spaceComplexity}</p></section>
    {[["javaSolution","Java"],["cppSolution","C++"],["pythonSolution","Python"]].map(([key,label]) => editorial[key] && <details key={key}><summary>{label}</summary><pre>{editorial[key]}</pre></details>)}
  </div>;
}
