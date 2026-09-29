# RECALL-X Content Deliverables

As highlighted in the v2 review, a hackathon isn't just about the code. To win, every team member needs an article, a social post, and a video that clearly communicates the innovation.

Here are the pre-written templates designed to maximize your score on the "Innovation" and "Real-world impact" criteria.

---

## 1. Technical Article (Medium / Dev.to)

**Title:** Beyond Chatbots: How We Built an AI Agent That Learns from Its Own Mistakes
**Subtitle:** Why building RECALL-X taught us that "Organizational Memory" is more about retaining *intent* than predicting failures.

**Draft Body:**
Every engineering team has experienced it: an incident occurs, an engineer spends three hours debugging, a fix is applied, and the lesson is immediately buried in a Slack thread. Three months later, a new hire deploys a change that undoes the fix, and the incident repeats. 

Companies don’t just lose systems when incidents happen—they lose the knowledge of what they learned from them.

For HackWithHyderabad 3.0, we wanted to build an AI agent that didn't just summarize logs, but actually retained the *intent* behind architecture decisions. We built **RECALL-X**.

**The Challenge with "Failure Prediction"**
Many AI coding tools promise to "predict production failures." We quickly realized this is a trap. AI cannot definitively predict if a config change will bring down a payment gateway. But what it *can* do is surface Chesterton’s Fence. 

If a junior engineer tries to raise the HikariCP `maximum-pool-size` to 60, our agent doesn't say "This will fail." Instead, it says: *"This touches a setting deliberately chosen in ADR-7 after INC-18. 60 connections across 4 replicas exceeds our MySQL max_connections limit of 150."* 

It warns based on historical similarity, not hallucinated predictions.

**Closing the Learning Loop**
The biggest technical hurdle wasn't the AI—it was the alert fatigue. If an AI agent warns an engineer and it's a false positive, the engineer will ignore the agent forever. 

To solve this, we implemented a strict feedback loop. Every time RECALL-X issues a warning, the engineer grades it. If it's marked a `FALSE_POSITIVE`, that outcome is ingested back into the Hindsight memory layer. The next time a similar deployment happens, the agent *remembers that it was wrong last time* and suppresses the warning. 

By tracking warning precision (Useful ÷ Total Warnings), we proved that RECALL-X actually gets smarter every single week.

---

## 2. Social Media Post (LinkedIn / X)

**For LinkedIn:**
Engineering teams keep paying for lessons they’ve already learned. 🔄

After an incident, the root cause, the failed attempts, and the final architecture decisions usually end up scattered across Jira tickets and postmortems that nobody ever rereads. When people move on, the knowledge leaves with them.

For HackWithHyderabad 3.0, our team built **RECALL-X**, a memory-first engineering agent powered by Hindsight. 

Instead of building just another generic AI chatbot, we built a system that actively guards your codebase. 
🛡️ **Decision Guard:** Touched a config setting that caused an outage 6 months ago? RECALL-X surfaces the exact incident and the reason the setting was locked in the first place before you deploy.
🧠 **Self-Correcting Warnings:** Alert fatigue kills AI tools. RECALL-X learns from its own false positives, improving its warning precision week-over-week.

Check out our 60-second demo below to see what happens when you give an AI agent true organizational memory! 👇

#HackWithHyderabad #AIAgents #EngineeringCulture #Hindsight #DevOps

---

## 3. Demo Video Script (60 Seconds)

**Visuals & Actions:**
- **[0:00 - 0:08]** Screen recording starts on the RECALL-X Dashboard. The camera pans across the realistic seeded metrics: *124 Experiences, 45 Incidents, 90% Warning Precision*.
- **[0:08 - 0:22]** *Visual: Risk Analysis page. The "Hindsight Memory" toggle is set to OFF.* An engineer pastes a deployment diff changing `maximum-pool-size` from 20 to 60. Clicks Analyze. The system outputs a generic "Proceed with deployment."
- **[0:22 - 0:40]** *Visual: The engineer clicks the "Hindsight Memory" toggle to ON and hits Analyze again.* The screen turns into a red `MEMORY-BASED RISK DETECTED` warning. It visibly traces the lineage to `ADR-7` and `INC-1024`, explicitly showing what failed previously.
- **[0:40 - 0:54]** *Visual: The engineer clicks "False Positive" on the feedback widget.* The screen cuts back to the Dashboard, showing the Warning Precision metric update.
- **[0:54 - 1:00]** *Visual: Team logo and RECALL-X title screen.*

**Voiceover Script:**
*"Engineering teams keep paying for lessons they’ve already learned. We built RECALL-X to stop that.*

*This is our dashboard. It doesn't just track incidents; it tracks our organizational memory.*

*Watch what happens when an engineer tries to raise our database connection pool to 60. Without memory, the AI just greenlights the change.*

*But when we turn Hindsight Memory ON... RECALL-X immediately flags a risk. It doesn't guess; it cites the exact Architecture Decision Record and the past incident where this exact change caused a MySQL outage.*

*And the best part? If the agent gets it wrong, you flag it as a false positive. RECALL-X ingests that mistake into its memory bank, meaning its precision actively improves over time.*

*Companies don't just lose systems when incidents happen—they lose the knowledge of what they learned from them. RECALL-X brings it back."*
