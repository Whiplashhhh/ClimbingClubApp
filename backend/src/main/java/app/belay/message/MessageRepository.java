package app.belay.message;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            join fetch m.sender
            where m.conversation.id = :conversationId
            order by m.createdAt asc, m.id asc
            """)
    List<Message> findThread(@Param("conversationId") UUID conversationId);

    /**
     * Non-lus par fil pour l'appelant : messages des autres postés après sa dernière lecture du
     * fil ({@link ConversationRead}), ou tous s'il ne l'a jamais ouvert.
     */
    @Query("""
            select m.conversation.id as conversationId, count(m.id) as total
            from Message m
            left join ConversationRead r on r.conversation.id = m.conversation.id and r.user.id = :userId
            where m.conversation.id in :conversationIds
              and m.sender.id <> :userId
              and (r.lastReadAt is null or m.createdAt > r.lastReadAt)
            group by m.conversation.id
            """)
    List<UnreadCount> countUnreadByConversation(
            @Param("userId") UUID userId, @Param("conversationIds") List<UUID> conversationIds);

    /** Dernier message de chaque fil donné (aperçu de liste), corrélé sur {@code createdAt}. */
    @Query("""
            select m from Message m
            join fetch m.sender
            where m.conversation.id in :conversationIds
              and m.createdAt = (
                  select max(m2.createdAt) from Message m2
                  where m2.conversation.id = m.conversation.id)
            """)
    List<Message> findLatestPerConversation(@Param("conversationIds") List<UUID> conversationIds);

    interface UnreadCount {
        UUID getConversationId();

        long getTotal();
    }
}
