-- Find duplicate hole entries for Eric Girard
SELECT 
    p.id as player_id,
    p.first_name,
    p.last_name,
    h.hole_number,
    COUNT(*) as duplicate_count,
    ps.id as scorecard_id,
    ps.gross_score,
    ps.net_score,
    ps.game_points,
    ps.created_at
FROM player_scorecard ps
JOIN player p ON ps.player_id = p.id
JOIN hole h ON ps.hole_id = h.id
WHERE p.first_name = 'Eric' AND p.last_name = 'Girard'
GROUP BY h.hole_number, p.id
HAVING COUNT(*) > 1
ORDER BY h.hole_number;
