select
    count(P.user_id),
    T.theme_name
    From s.conference_theme T
    join s.conference_participation P on P.participation_id = T.theme_id where T.theme_name = 'ИТ'
GROUP BY theme_name HAVING count(P.user_id) = 1;

select
    T.theme_name,
    C.event_date,
    C.meeting_link,
    S.status_name,
    count(P.user_id),
    sum(CASE WHEN P.is_attended = true then 1 else 0 end),
    R.type_name
    FROM s.conference_theme T
JOIN s.conference C on T.theme_id = C.theme_id
JOIN s.rule_types R on  T.theme_id = R.type_id
JOIN s.users U on T.user_id = U.user_id
JOIN s.conference_status S on C.status_id = S.status_id
JOIN s.conference_participation P on C.conference_id = P.conference_id
GROUP BY T.theme_name, C.event_date, C.meeting_link, S.status_name, R.type_name;


select distinct
    U.telegram_id,
    U.full_name,
    U.registration_date,
    PR.profession_name,
    sum(P.points) over(partition by P.user_id),
    count(P.user_id) over(partition by P.conference_id)
FROM s.users U
         JOIN s.conference_participation P on U.user_id = P.user_id
         JOIN s.profession PR on U.profession_id = PR.profession_id order by telegram_id desc ,full_name
desc ;


select
    U.telegram_id,
    U.full_name,
    U.registration_date,
    PR.profession_name,
    sum(P.points),
    count(P.user_id)
    FROM s.users U
JOIN s.conference_participation P on U.user_id = P.user_id
JOIN s.profession PR on U.profession_id = PR.profession_id
GROUP BY U.telegram_id, U.full_name, U.registration_date, PR.profession_name order by telegram_id;





select row_number() over (),
       US.username,
       TB.fine_amount,
       TB.points,
       VT.worst_moderator_id
    FROM s.conference_participation TB
JOIN s.users US on TB.user_id = US.user_id
JOIN s.vote  VT on US.user_id = VT.best_moderator_id;


select
    CT.user_id,
    (select  username FROM s.users US where Us.user_id=CT.user_id),
    CT.fine_amount,
    CT.points
    FROM s.conference_participation CT;







select US.username, CT.theme_name,vote_theme
FROM   s.conference_theme CT
join s.users US on US.user_id=Ct.user_id;

select
    (select username From s.users US where US.user_id=CT.user_id ),
    CT.theme_name,
    CT.vote_theme
FROm s.conference_theme CT;


