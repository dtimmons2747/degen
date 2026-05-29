-- Fix script to insert missing handicaps for players 2, 3, and 4
-- This script should be run after verifying that player1 handicaps were inserted

-- Insert tournament handicaps for player 2 (that haven't been inserted yet)
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player2_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player2_id = th.player_id
WHERE rtt.player2_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player2_id
    );

-- Insert tournament handicaps for player 3 (that haven't been inserted yet)
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player3_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player3_id = th.player_id
WHERE rtt.player3_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player3_id
    );

-- Insert tournament handicaps for player 4 (that haven't been inserted yet)
INSERT INTO round_handicap (round_tee_time_id, player_id, handicap)
SELECT 
    rtt.round_tee_time_id,
    rtt.player4_id,
    th.handicap
FROM round_tee_time rtt
INNER JOIN tournament_round tr ON rtt.tournament_round_id = tr.tournament_round_id
INNER JOIN tournament_handicap th ON tr.tournament_id = th.tournament_id AND rtt.player4_id = th.player_id
WHERE rtt.player4_id IS NOT NULL
    AND th.handicap IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM round_handicap rh 
        WHERE rh.round_tee_time_id = rtt.round_tee_time_id 
        AND rh.player_id = rtt.player4_id
    );

-- Verify the fix
SELECT 
    rtt.round_tee_time_id,
    rtt.player1_id,
    rtt.player2_id,
    rtt.player3_id,
    rtt.player4_id,
    SUM(CASE WHEN rh.player_id = rtt.player1_id THEN 1 ELSE 0 END) as has_p1_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player2_id THEN 1 ELSE 0 END) as has_p2_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player3_id THEN 1 ELSE 0 END) as has_p3_handicap,
    SUM(CASE WHEN rh.player_id = rtt.player4_id THEN 1 ELSE 0 END) as has_p4_handicap
FROM round_tee_time rtt
LEFT JOIN round_handicap rh ON rtt.round_tee_time_id = rh.round_tee_time_id
GROUP BY rtt.round_tee_time_id
LIMIT 20;
