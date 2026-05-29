-- Verification script to diagnose round_handicap migration

-- Check total counts
SELECT 
    'Total tee times' as metric,
    COUNT(*) as count
FROM round_tee_time
UNION ALL
SELECT 
    'Total round handicaps',
    COUNT(*)
FROM round_handicap
UNION ALL
SELECT 
    'Tee times with at least 1 handicap',
    COUNT(DISTINCT round_tee_time_id)
FROM round_handicap;

-- Check which player positions are NULL
SELECT 
    'player1_id NOT NULL' as metric,
    COUNT(*) as count
FROM round_tee_time
WHERE player1_id IS NOT NULL
UNION ALL
SELECT 
    'player2_id NOT NULL',
    COUNT(*)
FROM round_tee_time
WHERE player2_id IS NOT NULL
UNION ALL
SELECT 
    'player3_id NOT NULL',
    COUNT(*)
FROM round_tee_time
WHERE player3_id IS NOT NULL
UNION ALL
SELECT 
    'player4_id NOT NULL',
    COUNT(*)
FROM round_tee_time
WHERE player4_id IS NOT NULL;

-- Detailed breakdown - show each tee time with its players and handicaps
SELECT 
    rtt.round_tee_time_id,
    rtt.player1_id,
    rtt.player2_id,
    rtt.player3_id,
    rtt.player4_id,
    SUM(CASE WHEN rh.player_id = rtt.player1_id THEN 1 ELSE 0 END) as has_p1_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player2_id THEN 1 ELSE 0 END) as has_p2_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player3_id THEN 1 ELSE 0 END) as has_p3_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player4_id THEN 1 ELSE 0 END) as has_p4_handicap,
    GROUP_CONCAT(rh.player_id, ':', rh.handicap) as round_handicaps
FROM round_tee_time rtt
LEFT JOIN round_handicap rh ON rtt.round_tee_time_id = rh.round_tee_time_id
GROUP BY rtt.round_tee_time_id
ORDER BY rtt.round_tee_time_id
LIMIT 20;

-- Check for player ID mismatches - handicaps for players not in the tee time
SELECT 
    rh.round_tee_time_id,
    rh.player_id,
    rh.handicap,
    rtt.player1_id,
    rtt.player2_id,
    rtt.player3_id,
    rtt.player4_id,
    'MISMATCH' as issue
FROM round_handicap rh
INNER JOIN round_tee_time rtt ON rh.round_tee_time_id = rtt.round_tee_time_id
WHERE rh.player_id NOT IN (
    rtt.player1_id, rtt.player2_id, rtt.player3_id, rtt.player4_id
)
LIMIT 20;
