Step 1 is complete.

Current status:
- domain/model is now extracted into [describe module/package setup]
- these tests pass: [...]
- these issues/temporary decisions remain: [...]
- attached are the current files after step 1

Now continue with step 2 only.

Your task:
0. In BaseOutcomeSelector, instead of argmax -> value -> threshold, should be more like score = (market - public) * exp(-k *(1-prob)), base_pick = argmax(score)
1. DecisionParameters and AdjustmentWeights disappear at first but similar logic may reappear later with Tags/Buffs
2. Recent home form score disappears. 
1. analyze the current extracted domain/model module
2. propose the smallest safe redesign toward the new ensemble-based model
3. ensure the model outputs both:
    - final selection result
    - full internal probabilities for each match
4. identify which classes to rename/remove/change first
5. propose the JUnit test plan before coding
6. do not refactor everything at once
7. preserve working behavior where not explicitly changed